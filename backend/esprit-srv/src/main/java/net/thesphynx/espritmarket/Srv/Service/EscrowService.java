package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.EscrowHold;
import net.thesphynx.espritmarket.Srv.Entity.Wallet;
import net.thesphynx.espritmarket.Srv.Entity.WalletTransaction;
import net.thesphynx.espritmarket.Srv.Repository.IBookingRepository;
import net.thesphynx.espritmarket.Srv.Repository.IEscrowHoldRepository;
import net.thesphynx.espritmarket.Srv.Repository.IWalletRepository;
import net.thesphynx.espritmarket.Srv.Repository.IWalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EscrowService {

    private final IWalletRepository walletRepository;
    private final IWalletTransactionRepository txRepository;
    private final IEscrowHoldRepository escrowRepository;
    private final IBookingRepository bookingRepository;
    private final UserRepository userRepository;

    public EscrowService(IWalletRepository walletRepository,
                         IWalletTransactionRepository txRepository,
                         IEscrowHoldRepository escrowRepository,
                         IBookingRepository bookingRepository,
                         UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.txRepository = txRepository;
        this.escrowRepository = escrowRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setBalance(0.0);
            return walletRepository.save(wallet);
        });
    }

    @Transactional
    public Wallet topUp(Long userId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType("TOP_UP");
        tx.setAmount(amount);
        tx.setDescription("Wallet top-up");
        txRepository.save(tx);

        return wallet;
    }

    @Transactional
    public EscrowHold createHold(Long bookingId, Long userId) {
        if (escrowRepository.existsByBookingId(bookingId)) {
            return escrowRepository.findByBookingId(bookingId).orElseThrow();
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        Wallet wallet = getOrCreateWallet(userId);
        double amount = booking.getTotalPrice() != null ? booking.getTotalPrice().doubleValue() : booking.getDuration() * booking.getService().getPrice().doubleValue();
        if (amount <= 0) amount = 50.0;

        if (wallet.getBalance() < amount) {
            throw new IllegalStateException("Insufficient wallet balance. Required: " + amount + ", Available: " + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        WalletTransaction debitTx = new WalletTransaction();
        debitTx.setWallet(wallet);
        debitTx.setType("ESCROW_HOLD");
        debitTx.setAmount(-amount);
        debitTx.setDescription("Escrow hold for booking #" + bookingId);
        debitTx.setReferenceType("BOOKING");
        debitTx.setReferenceId(bookingId);
        txRepository.save(debitTx);

        EscrowHold hold = new EscrowHold();
        hold.setBooking(booking);
        hold.setWallet(wallet);
        hold.setAmount(amount);
        hold.setStatus("HELD");
        hold.setReleaseDeadline(LocalDateTime.now().plusDays(30));
        escrowRepository.save(hold);

        return hold;
    }

    @Transactional
    public EscrowHold release(Long bookingId) {
        EscrowHold hold = escrowRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No escrow hold found"));

        if (!hold.getStatus().equals("HELD")) {
            throw new IllegalStateException("Escrow is not in HELD state");
        }

        hold.setStatus("RELEASED");
        hold.setReleasedAt(LocalDateTime.now());
        escrowRepository.save(hold);

        Long providerId = hold.getBooking().getProvider().getId();
        Wallet providerWallet = getOrCreateWallet(providerId);
        providerWallet.setBalance(providerWallet.getBalance() + hold.getAmount());
        walletRepository.save(providerWallet);

        WalletTransaction creditTx = new WalletTransaction();
        creditTx.setWallet(providerWallet);
        creditTx.setType("ESCROW_RELEASE");
        creditTx.setAmount(hold.getAmount());
        creditTx.setDescription("Escrow release from booking #" + bookingId);
        creditTx.setReferenceType("BOOKING");
        creditTx.setReferenceId(bookingId);
        txRepository.save(creditTx);

        return hold;
    }

    @Transactional
    public EscrowHold refund(Long bookingId) {
        EscrowHold hold = escrowRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("No escrow hold found"));

        if (!hold.getStatus().equals("HELD")) {
            throw new IllegalStateException("Escrow is not in HELD state");
        }

        hold.setStatus("REFUNDED");
        hold.setReleasedAt(LocalDateTime.now());
        escrowRepository.save(hold);

        Wallet wallet = hold.getWallet();
        wallet.setBalance(wallet.getBalance() + hold.getAmount());
        walletRepository.save(wallet);

        WalletTransaction refundTx = new WalletTransaction();
        refundTx.setWallet(wallet);
        refundTx.setType("ESCROW_REFUND");
        refundTx.setAmount(hold.getAmount());
        refundTx.setDescription("Escrow refund for booking #" + bookingId);
        refundTx.setReferenceType("BOOKING");
        refundTx.setReferenceId(bookingId);
        txRepository.save(refundTx);

        return hold;
    }

    @Transactional
    public int processAutoReleases() {
        List<EscrowHold> overdue = escrowRepository.findOverdueHolds("HELD", LocalDateTime.now());
        int count = 0;
        for (EscrowHold hold : overdue) {
            hold.setStatus("AUTO_RELEASED");
            hold.setReleasedAt(LocalDateTime.now());
            escrowRepository.save(hold);

            Long providerId = hold.getBooking().getProvider().getId();
            Wallet providerWallet = getOrCreateWallet(providerId);
            providerWallet.setBalance(providerWallet.getBalance() + hold.getAmount());
            walletRepository.save(providerWallet);

            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(providerWallet);
            tx.setType("AUTO_RELEASE");
            tx.setAmount(hold.getAmount());
            tx.setDescription("Auto-release (30 days) from booking #" + hold.getBooking().getId());
            tx.setReferenceType("BOOKING");
            tx.setReferenceId(hold.getBooking().getId());
            txRepository.save(tx);

            count++;
        }
        return count;
    }

    public Optional<EscrowHold> getHold(Long bookingId) {
        return escrowRepository.findByBookingId(bookingId);
    }

    public List<WalletTransaction> getTransactions(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return txRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
}
