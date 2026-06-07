package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.Exception.BadRequestException;
import net.thesphynx.espritmarket.Srv.Dto.ProviderExceptionRequest;
import net.thesphynx.espritmarket.Srv.Dto.ProviderExceptionResponse;
import net.thesphynx.espritmarket.Srv.Entity.ProviderExceptionType;
import net.thesphynx.espritmarket.Srv.Mapper.ProviderExceptionMapper;
import net.thesphynx.espritmarket.Srv.Repository.IProviderExceptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ProviderExceptionService {
    private final IProviderExceptionRepository exceptionRepository;
    private final ProviderExceptionMapper exceptionMapper;

    public ProviderExceptionService(IProviderExceptionRepository exceptionRepository,
                                    ProviderExceptionMapper exceptionMapper) {
        this.exceptionRepository = exceptionRepository;
        this.exceptionMapper = exceptionMapper;
    }

    public List<ProviderExceptionResponse> getByProvider(Long providerId) {
        return exceptionRepository.findByProviderId(providerId).stream()
                .map(exceptionMapper::toResponse)
                .toList();
    }

    public List<ProviderExceptionResponse> getByDateRange(Long providerId, LocalDate startDate, LocalDate endDate) {
        return exceptionRepository.findByProviderIdAndDateBetween(providerId, startDate, endDate).stream()
                .map(exceptionMapper::toResponse)
                .toList();
    }

    public ProviderExceptionResponse create(ProviderExceptionRequest request) {
        if (request.getType() == ProviderExceptionType.CUSTOM_HOURS
                && (request.getStartHour() == null || request.getEndHour() == null)) {
            throw new BadRequestException("CUSTOM_HOURS exceptions require startHour and endHour");
        }
        var entity = exceptionMapper.toEntity(request);
        return exceptionMapper.toResponse(exceptionRepository.save(entity));
    }

    public Optional<ProviderExceptionResponse> update(Long id, ProviderExceptionRequest request) {
        return exceptionRepository.findById(id)
                .map(existing -> {
                    var entity = exceptionMapper.toEntity(request);
                    entity.setId(id);
                    return exceptionMapper.toResponse(exceptionRepository.save(entity));
                });
    }

    public boolean delete(Long id) {
        if (!exceptionRepository.existsById(id)) return false;
        exceptionRepository.deleteById(id);
        return true;
    }
}
