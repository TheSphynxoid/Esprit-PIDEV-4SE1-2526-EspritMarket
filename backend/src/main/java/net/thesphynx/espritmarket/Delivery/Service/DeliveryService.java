package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Delivery.Entity.Delivery;
import net.thesphynx.espritmarket.Delivery.Entity.DeliveryMode;
import net.thesphynx.espritmarket.Delivery.Entity.MapTracking;
import net.thesphynx.espritmarket.Delivery.Entity.Vehicule;
import net.thesphynx.espritmarket.Delivery.Repository.IDeliveryRepository;
import net.thesphynx.espritmarket.Marketplace.Entity.Order;
import net.thesphynx.espritmarket.Marketplace.Repository.IOrderRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.LinkedHashMap;
import net.thesphynx.espritmarket.Delivery.Dto.DeliveryAddressViewDto;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;

import net.thesphynx.espritmarket.Delivery.Entity.Delivery;
import net.thesphynx.espritmarket.Delivery.Repository.IDeliveryRepository;
import net.thesphynx.espritmarket.Marketplace.Entity.Order;
import net.thesphynx.espritmarket.Marketplace.Repository.IOrderRepository;

@Service
public class DeliveryService {
    private static final BigDecimal COST_PER_KM = new BigDecimal("0.500");
    private static final BigDecimal ZERO = new BigDecimal("0.000");
    private static final BigDecimal FISCAL_STAMP_FEE = new BigDecimal("1.000");
    // New configuration per user request
    private static final BigDecimal WEIGHT_RATE = new BigDecimal("0.4");
    private static final BigDecimal MINIMUM_PRICE = new BigDecimal("1.000");
    private static final Map<String, BigDecimal> DELIVERY_TYPE_COEFFICIENTS = new HashMap<>();

    static {
        DELIVERY_TYPE_COEFFICIENTS.put("STANDARD", new BigDecimal("1.0"));
        DELIVERY_TYPE_COEFFICIENTS.put("EXPRESS", new BigDecimal("1.3"));
        DELIVERY_TYPE_COEFFICIENTS.put("SAME_DAY", new BigDecimal("1.6"));
    }

    private final IDeliveryRepository deliveryRepository;
    private final IOrderRepository orderRepository;

    public DeliveryService(IDeliveryRepository deliveryRepository,
                           IOrderRepository orderRepository) {
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
    }

    public List<Delivery> getAll() {
        return deliveryRepository.findAll();
    }

    public List<Delivery> getAllForUser(Long connectedUserId) {
        List<Delivery> ownedDeliveries = deliveryRepository.findByConnectedUserIdOrderByIdDesc(connectedUserId);
        List<Delivery> orderUserDeliveries = deliveryRepository.findDistinctByOrder_User_IdOrderByIdDesc(connectedUserId);

        LinkedHashMap<Long, Delivery> deliveriesById = new LinkedHashMap<>();
        for (Delivery delivery : ownedDeliveries) {
            deliveriesById.put(delivery.getId(), delivery);
        }
        for (Delivery delivery : orderUserDeliveries) {
            deliveriesById.putIfAbsent(delivery.getId(), delivery);
        }

        return deliveriesById.values().stream()
                .sorted((left, right) -> Long.compare(right.getId(), left.getId()))
                .collect(Collectors.toList());
    }

    public Optional<Delivery> getById(Long id) {
        return deliveryRepository.findById(id);
    }

    public List<DeliveryAddressViewDto> getAddressViewsByConnectedUserId(Long connectedUserId) {
        return deliveryRepository.findByConnectedUserIdOrderByIdDesc(connectedUserId)
                .stream()
                .map(this::toAddressView)
                .filter(this::hasAtLeastOneAddressField)
                .collect(Collectors.toList());
    }

    public Delivery create(Delivery delivery) {
        assignQrTokenIfMissing(delivery);
        attachManagedOrderAndValidate(delivery, null);
        
        // Capture base order amount (products only) at delivery creation time
        // This prevents issues where order.getTotalAmount() gets updated after delivery is created
        Order order = delivery.getOrder() != null && delivery.getOrder().getId() != null
            ? orderRepository.findById(delivery.getOrder().getId()).orElse(null)
            : null;
            
        if (order != null) {
            BigDecimal baseAmount = calculateBaseOrderAmountFromOrderLines(order);
            delivery.setBaseOrderAmount(baseAmount);
        } else {
            delivery.setBaseOrderAmount(BigDecimal.ZERO);
        }
        
        Delivery savedDelivery = deliveryRepository.save(delivery);
        syncOrderWithDelivery(savedDelivery);
        return savedDelivery;
    }

    public Delivery update(Long id, Delivery delivery) {
        delivery.setId(id);
        attachManagedOrderAndValidate(delivery, id);
        Delivery updatedDelivery = deliveryRepository.save(delivery);
        syncOrderWithDelivery(updatedDelivery);
        return updatedDelivery;
    }

    public Optional<Delivery> assignVehicule(Long deliveryId, Long vehiculeId) {
        return deliveryRepository.findById(deliveryId).map(existing -> {
            Vehicule vehicule = new Vehicule();
            vehicule.setId(vehiculeId);
            existing.setVehicule(vehicule);
            return deliveryRepository.save(existing);
        });
    }

    public Optional<Delivery> markDeliveredByQrToken(String qrToken) {
        if (qrToken == null || qrToken.isBlank()) {
            return Optional.empty();
        }

        return deliveryRepository.findByQrToken(qrToken).map(existing -> {
            existing.setStatus("Delivered");
            Delivery updatedDelivery = deliveryRepository.save(existing);
            syncOrderWithDelivery(updatedDelivery);
            return updatedDelivery;
        });
    }

    public Optional<MapTracking> updateTrackingForDelivery(Long deliveryId, MapTracking tracking) {
        return deliveryRepository.findById(deliveryId).map(existing -> {
            if (existing.getTracking() != null && existing.getTracking().getId() != null) {
                tracking.setId(existing.getTracking().getId());
            }
            existing.setTracking(tracking);
            Delivery updatedDelivery = deliveryRepository.save(existing);
            return updatedDelivery.getTracking();
        });
    }

    public Optional<MapTracking> getTrackingForDelivery(Long deliveryId) {
        return deliveryRepository.findById(deliveryId).map(Delivery::getTracking);
    }

    public byte[] generateQrCodePng(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 320, 320);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
                return outputStream.toByteArray();
            }
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("Unable to generate QR code", exception);
        }
    }

    public void delete(Long id) {
        deliveryRepository.deleteById(id);
    }

    public Delivery ensureQrToken(Delivery delivery) {
        if (delivery.getQrToken() == null || delivery.getQrToken().isBlank()) {
            delivery.setQrToken(UUID.randomUUID().toString());
            return deliveryRepository.save(delivery);
        }

        return delivery;
    }

    public void initializeDeliveryTracking(Order order) {
        if (order.getDelivery() != null) {
            return;
        }

        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setStatus("PENDING");

        deliveryRepository.save(delivery);
    }

    private void assignQrTokenIfMissing(Delivery delivery) {
        if (delivery.getQrToken() == null || delivery.getQrToken().isBlank()) {
            delivery.setQrToken(UUID.randomUUID().toString());
        }
    }

    private void attachManagedOrderAndValidate(Delivery delivery, Long deliveryId) {
        if (delivery.getOrder() == null || delivery.getOrder().getId() == null) {
            return;
        }

        Long orderId = delivery.getOrder().getId();
        Order managedOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found with id " + orderId));

        deliveryRepository.findByOrder_Id(orderId)
                .filter(existing -> deliveryId == null || !existing.getId().equals(deliveryId))
                .ifPresent(existing -> {
                    throw new IllegalStateException("A delivery already exists for order " + orderId);
                });

        delivery.setOrder(managedOrder);
    }

    private void syncOrderWithDelivery(Delivery delivery) {
        if (delivery.getOrder() != null && delivery.getOrder().getId() != null) {
            var order = orderRepository.findById(delivery.getOrder().getId());
            if (order.isPresent()) {
                Order currentOrder = order.get();
                currentOrder.setStatus(delivery.getStatus());

                // Use the baseOrderAmount we stored at delivery creation time
                BigDecimal baseOrderAmount = delivery.getBaseOrderAmount() != null ? delivery.getBaseOrderAmount() : BigDecimal.ZERO;
                BigDecimal deliveryFee = calculateDeliveryFee(delivery);
                BigDecimal finalAmount = baseOrderAmount
                    .add(deliveryFee)
                        .setScale(3, RoundingMode.HALF_UP);

                currentOrder.setTotalAmount(finalAmount.doubleValue());
                orderRepository.save(currentOrder);
            }
        }
    }

    private BigDecimal resolveBaseOrderAmount(Order order) {
        // This should not be called anymore - we use delivery.baseOrderAmount instead
        // But keeping it for backward compatibility
        return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBaseOrderAmountFromOrderLines(Order order) {
        // Calculate product subtotal from orderLines (products only, no delivery fee)
        if (order != null && order.getOrderLines() != null && !order.getOrderLines().isEmpty()) {
            double sumSubtotals = order.getOrderLines().stream()
                    .filter(line -> line.getSubtotal() != null)
                    .mapToDouble(line -> line.getSubtotal())
                    .sum();
            if (sumSubtotals > 0) {
                return BigDecimal.valueOf(sumSubtotals).setScale(3, RoundingMode.HALF_UP);
            }
        }

        // Fallback: use getTotalAmount() which is the order subtotal at creation time
        // At this point, delivery hasn't been created yet, so totalAmount is still just products
        if (order != null && order.getTotalAmount() != null && order.getTotalAmount() > 0) {
            return BigDecimal.valueOf(order.getTotalAmount()).setScale(3, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDeliveryFee(Delivery delivery) {
        if (!DeliveryMode.LIVRAISON_A_DOMICILE.equals(delivery.getDeliveryMode())) {
            return ZERO;
        }

        BigDecimal distanceKm = delivery.getDistanceKm() != null ? delivery.getDistanceKm() : ZERO;
        if (distanceKm.compareTo(ZERO) < 0) {
            distanceKm = ZERO;
        }

        // base price preserved as requested: 1.000 + 0.500 * distanceKm
        BigDecimal basePrice = FISCAL_STAMP_FEE.add(COST_PER_KM.multiply(distanceKm));

        // compute volumetric weight = (length * width * height) / 5000
        BigDecimal length = delivery.getLength() != null ? delivery.getLength() : ZERO;
        BigDecimal width = delivery.getWidth() != null ? delivery.getWidth() : ZERO;
        BigDecimal height = delivery.getHeight() != null ? delivery.getHeight() : ZERO;

        BigDecimal volume = length.multiply(width).multiply(height);
        BigDecimal volumetricWeight = BigDecimal.ZERO;
        if (volume.compareTo(BigDecimal.ZERO) > 0) {
            volumetricWeight = volume.divide(new BigDecimal("5000"), 6, RoundingMode.HALF_UP);
        }

        BigDecimal realWeight = delivery.getRealWeight() != null ? delivery.getRealWeight() : ZERO;
        BigDecimal chargeableWeight = realWeight.max(volumetricWeight);

        BigDecimal weightCharge = chargeableWeight.multiply(WEIGHT_RATE);

        BigDecimal coefficient = getDeliveryTypeCoefficient(delivery.getDeliverytype());

        BigDecimal finalPrice = basePrice.add(weightCharge).multiply(coefficient).setScale(3, RoundingMode.HALF_UP);

        if (finalPrice.compareTo(MINIMUM_PRICE) < 0) {
            finalPrice = MINIMUM_PRICE;
        }

        return finalPrice;
    }

    private BigDecimal getDeliveryTypeCoefficient(String deliveryType) {
        if (deliveryType == null) {
            return DELIVERY_TYPE_COEFFICIENTS.getOrDefault("STANDARD", BigDecimal.ONE);
        }
        return DELIVERY_TYPE_COEFFICIENTS.getOrDefault(deliveryType.toUpperCase(), BigDecimal.ONE);
    }

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Delivery delivery) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                drawInvoiceHeader(cs, doc, delivery);

                Order order = loadManagedOrder(delivery);
                BigDecimal deliveryFee = calculateDeliveryFee(delivery);
                BigDecimal baseOrderAmount = delivery.getBaseOrderAmount() != null ? delivery.getBaseOrderAmount() : BigDecimal.ZERO;
                BigDecimal finalAmount = baseOrderAmount.add(deliveryFee).setScale(3, RoundingMode.HALF_UP);

                // Start content lower to increase top margin and make header clearer
                float y = 540;
                y = drawInvoiceMeta(cs, delivery, order, y);
                y = drawSectionTitle(cs, "Détails de livraison", y);
                y = drawKeyValue(cs, "Adresse", safe(delivery.getDeliveryAddress()), y);
                y = drawKeyValue(cs, "Ville", safe(delivery.getCity()), y);
                y = drawKeyValue(cs, "Code postal", safe(delivery.getPostalCode()), y);
                y = drawKeyValue(cs, "Téléphone", safe(delivery.getPhoneNumber()), y);
                y = drawKeyValue(cs, "Mode de livraison", delivery.getDeliveryMode() != null ? formatDeliveryMode(delivery.getDeliveryMode().name()) : "-", y);
                y = drawKeyValue(cs, "Type de livraison", formatDeliveryType(delivery.getDeliverytype()), y);
                y = drawKeyValue(cs, "Poids réel", formatKg(delivery.getRealWeight()), y);
                y = drawKeyValue(cs, "Poids volumétrique", formatKg(calculateVolumetricWeight(delivery)), y);
                y = drawKeyValue(cs, "Poids facturable", formatKg(calculateChargeableWeight(delivery)), y);
                y = drawKeyValue(cs, "Distance", formatKm(delivery.getDistanceKm()), y);

                y = drawSectionTitle(cs, "Produits commandés", y - 4);
                y = drawOrderLinesTable(cs, order, y);

                y = drawSectionTitle(cs, "Récapitulatif des prix", y - 4);
                y = drawSummaryLine(cs, "Sous-total produits", baseOrderAmount.toPlainString() + " DT", y);
                y = drawSummaryLine(cs, "Frais de livraison", deliveryFee.toPlainString() + " DT", y);
                y = drawSummaryLine(cs, "Total TTC", finalAmount.toPlainString() + " DT", y, true);

                y = drawSectionTitle(cs, "Paiement", y - 4);
                y = drawKeyValue(cs, "Mode de paiement", resolvePaymentLabel(delivery, order), y);
                y = drawKeyValue(cs, "Statut", safe(delivery.getStatus()), y);

                drawInvoiceFooterLogo(cs, doc);
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                doc.save(out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate invoice PDF", e);
        }
    }

    private void drawInvoiceHeader(PDPageContentStream cs, PDDocument doc, Delivery delivery) throws IOException {
        // Reserve larger top margin: draw header lower on the page so it appears with more whitespace above
        PDImageXObject logo = loadInvoiceLogo(doc);
        if (logo != null) {
            // Move logo lower to create a larger top margin; align logo top with title box top
            cs.drawImage(logo, 30, 640, 100, 100);
        }

        // Title box - positioned lower to increase top whitespace
        cs.setNonStrokingColor(255, 255, 255);
        cs.addRect(145, 650, 410, 90);
        cs.fill();

        // Border around title - thicker for visibility
        cs.setNonStrokingColor(160, 30, 30);
        cs.setLineWidth(2);
        cs.addRect(145, 650, 410, 90);
        cs.stroke();

        // Main title - positioned inside the title box (top - 20)
        cs.setNonStrokingColor(160, 30, 30);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 26);
        cs.newLineAtOffset(155, 706);
        cs.showText("ESPRIT MARKET");
        cs.endText();

        // Subtitle - positioned below main title
        cs.setNonStrokingColor(100, 100, 100);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 12);
        cs.newLineAtOffset(155, 686);
        cs.showText("Facture de commande et livraison");
        cs.endText();

        // Additional detail line
        cs.setNonStrokingColor(130, 130, 130);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.newLineAtOffset(155, 670);
        cs.showText("Invoice & Delivery Receipt");
        cs.endText();

        // Horizontal separator line - placed below the title and logo to avoid overlap
        cs.setStrokingColor(160, 30, 30);
        cs.setLineWidth(2);
        cs.moveTo(30, 600);
        cs.lineTo(560, 600);
        cs.stroke();

        // Invoice number - positioned below separator and to the left of the logo
        cs.setNonStrokingColor(0, 0, 0);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
        cs.newLineAtOffset(30, 580);
        cs.showText("Facture #" + (delivery.getId() != null ? delivery.getId() : "-"));
        cs.endText();
    }

    private PDImageXObject loadInvoiceLogo(PDDocument doc) {
        try (InputStream inputStream = getClass().getResourceAsStream("/logo.png")) {
            if (inputStream == null) {
                return null;
            }
            byte[] bytes = inputStream.readAllBytes();
            return PDImageXObject.createFromByteArray(doc, bytes, "logo");
        } catch (IOException exception) {
            return null;
        }
    }

    private float drawInvoiceMeta(PDPageContentStream cs, Delivery delivery, Order order, float y) throws IOException {
        String orderDate = order != null && order.getDate() != null
                ? DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH)
                .format(order.getDate().toInstant().atZone(ZoneId.systemDefault()))
                : "-";

        y = drawKeyValue(cs, "Date", orderDate, y);
        y = drawKeyValue(cs, "Delivery ID", delivery.getId() != null ? delivery.getId().toString() : "-", y);
        return y - 8;
    }

    private float drawSectionTitle(PDPageContentStream cs, String title, float y) throws IOException {
        cs.setNonStrokingColor(160, 30, 30);
        cs.addRect(40, y - 4, 515, 18);
        cs.fill();

        cs.setNonStrokingColor(255, 255, 255);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(48, y + 2);
        cs.showText(title);
        cs.endText();

        cs.setNonStrokingColor(0, 0, 0);
        return y - 24;
    }

    private float drawKeyValue(PDPageContentStream cs, String label, String value, float y) throws IOException {
        cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        cs.beginText();
        cs.newLineAtOffset(42, y);
        cs.showText(label + ":");
        cs.endText();

        cs.setFont(PDType1Font.HELVETICA, 9);
        cs.beginText();
        cs.newLineAtOffset(160, y);
        cs.showText(value);
        cs.endText();

        return y - 13;
    }

    private void drawInvoiceFooterLogo(PDPageContentStream cs, PDDocument doc) throws IOException {
        PDImageXObject footerImage = loadInvoiceFooterImage(doc);
        if (footerImage == null) {
            return;
        }

        float width = 120;
        float height = 120;
        float x = PDRectangle.LETTER.getWidth() - width - 28;
        float y = 24;
        cs.drawImage(footerImage, x, y, width, height);
    }

    private PDImageXObject loadInvoiceFooterImage(PDDocument doc) {
        try (InputStream inputStream = getClass().getResourceAsStream("/cache.png")) {
            if (inputStream == null) {
                return null;
            }
            byte[] bytes = inputStream.readAllBytes();
            return PDImageXObject.createFromByteArray(doc, bytes, "cache");
        } catch (IOException exception) {
            return null;
        }
    }

    private float drawOrderLinesTable(PDPageContentStream cs, Order order, float y) throws IOException {
        float startY = y;
        drawTableHeader(cs, startY);
        startY -= 16;

        if (order == null || order.getOrderLines() == null || order.getOrderLines().isEmpty()) {
            return drawKeyValue(cs, "Lignes", "Aucun article", startY) - 6;
        }

        int maxLines = 12;
        int index = 0;
        for (var line : order.getOrderLines()) {
            if (index++ >= maxLines) {
                startY = drawKeyValue(cs, "", "...", startY);
                break;
            }

            String productName = line.getProduct() != null ? safe(line.getProduct().getName()) : "-";
            String price = formatMoney(line.getPrice());
            String subtotal = formatMoney(line.getSubtotal());
            String row = String.format("%3d   %-34.34s   %8s   %8s", line.getQuantity(), productName, price, subtotal);

            cs.setFont(PDType1Font.HELVETICA, 8.5f);
            cs.beginText();
            cs.newLineAtOffset(42, startY);
            cs.showText(row);
            cs.endText();

            startY -= 13;
        }

        return startY - 6;
    }

    private void drawTableHeader(PDPageContentStream cs, float y) throws IOException {
        cs.setNonStrokingColor(245, 245, 245);
        cs.addRect(40, y - 2, 515, 15);
        cs.fill();

        cs.setNonStrokingColor(0, 0, 0);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 8.5f);
        cs.beginText();
        cs.newLineAtOffset(42, y + 2);
        cs.showText("Qté   Produit                               Prix U.   Sous-total");
        cs.endText();
    }

    private float drawSummaryLine(PDPageContentStream cs, String label, String value, float y) throws IOException {
        return drawSummaryLine(cs, label, value, y, false);
    }

    private float drawSummaryLine(PDPageContentStream cs, String label, String value, float y, boolean emphasized) throws IOException {
        cs.setFont(emphasized ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, emphasized ? 11 : 9);
        cs.beginText();
        cs.newLineAtOffset(42, y);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.newLineAtOffset(475, y);
        cs.showText(value);
        cs.endText();

        return y - (emphasized ? 16 : 12);
    }

    private Order loadManagedOrder(Delivery delivery) {
        if (delivery.getOrder() == null || delivery.getOrder().getId() == null) {
            return null;
        }

        return orderRepository.findById(delivery.getOrder().getId()).orElse(delivery.getOrder());
    }

    private String resolvePaymentLabel(Delivery delivery, Order order) {
        if (delivery.getPaymentMode() != null) {
            return delivery.getPaymentMode().getLabel();
        }

        return order != null && order.getPaymentMethod() != null
                ? order.getPaymentMethod()
                : "-";
    }

    private String formatDeliveryMode(String value) {
        return switch (safe(value).toUpperCase()) {
            case "LIVRAISON_A_DOMICILE" -> "Livraison à domicile";
            case "RETRAIT_A_ESPRIT" -> "Retrait à Esprit";
            default -> safe(value);
        };
    }

    private String formatDeliveryType(String value) {
        return switch (safe(value).toUpperCase()) {
            case "STANDARD" -> "Standard";
            case "EXPRESS" -> "Express";
            case "SAME_DAY" -> "Same day";
            default -> safe(value);
        };
    }

    private BigDecimal calculateVolumetricWeight(Delivery delivery) {
        BigDecimal length = delivery.getLength() != null ? delivery.getLength() : ZERO;
        BigDecimal width = delivery.getWidth() != null ? delivery.getWidth() : ZERO;
        BigDecimal height = delivery.getHeight() != null ? delivery.getHeight() : ZERO;
        BigDecimal volume = length.multiply(width).multiply(height);
        if (volume.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        return volume.divide(new BigDecimal("5000"), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChargeableWeight(Delivery delivery) {
        BigDecimal realWeight = delivery.getRealWeight() != null ? delivery.getRealWeight() : ZERO;
        return realWeight.max(calculateVolumetricWeight(delivery)).setScale(3, RoundingMode.HALF_UP);
    }

    private String formatKg(BigDecimal value) {
        return (value != null ? value.setScale(3, RoundingMode.HALF_UP).toPlainString() : "0.000") + " kg";
    }

    private String formatKm(BigDecimal value) {
        return (value != null ? value.setScale(3, RoundingMode.HALF_UP).toPlainString() : "0.000") + " km";
    }

    private String formatMoney(Double value) {
        return value == null ? "0.000" : BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private DeliveryAddressViewDto toAddressView(Delivery delivery) {
        String deliveryAddress = delivery.getDeliveryAddress();
        String city = delivery.getCity();
        String postalCode = delivery.getPostalCode();
        String phoneNumber = delivery.getPhoneNumber();

        if (delivery.getDeliveryAddressDetails() != null) {
            if (deliveryAddress == null || deliveryAddress.isBlank()) {
                deliveryAddress = delivery.getDeliveryAddressDetails().getDeliveryAddress();
            }
            if (city == null || city.isBlank()) {
                city = delivery.getDeliveryAddressDetails().getCity();
            }
            if (postalCode == null || postalCode.isBlank()) {
                postalCode = delivery.getDeliveryAddressDetails().getPostalCode();
            }
            if (phoneNumber == null || phoneNumber.isBlank()) {
                phoneNumber = delivery.getDeliveryAddressDetails().getPhoneNumber();
            }
        }

        return new DeliveryAddressViewDto(deliveryAddress, city, postalCode, phoneNumber);
    }

    private boolean hasAtLeastOneAddressField(DeliveryAddressViewDto addressView) {
        return (addressView.getDeliveryAddress() != null && !addressView.getDeliveryAddress().isBlank())
                || (addressView.getCity() != null && !addressView.getCity().isBlank())
                || (addressView.getPostalCode() != null && !addressView.getPostalCode().isBlank())
                || (addressView.getPhoneNumber() != null && !addressView.getPhoneNumber().isBlank());
    }
}
