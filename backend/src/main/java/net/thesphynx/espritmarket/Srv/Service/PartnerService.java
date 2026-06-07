package net.thesphynx.espritmarket.Srv.Service;

import net.thesphynx.espritmarket.Common.DTO.PageResponse;
import net.thesphynx.espritmarket.Srv.Dto.PartnerRequest;
import net.thesphynx.espritmarket.Srv.Dto.PartnerResponse;
import net.thesphynx.espritmarket.Srv.Entity.Partner;
import net.thesphynx.espritmarket.Srv.Mapper.PartnerMapper;
import net.thesphynx.espritmarket.Srv.Repository.IPartnerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PartnerService {
    private final IPartnerRepository partnerRepository;
    private final PartnerMapper partnerMapper;

    public PartnerService(IPartnerRepository partnerRepository, PartnerMapper partnerMapper) {
        this.partnerRepository = partnerRepository;
        this.partnerMapper = partnerMapper;
    }

    public PageResponse<PartnerResponse> getAll(int page, int size) {
        Page<Partner> result = partnerRepository.findAllActive(PageRequest.of(page, size));
        return toPageResponse(result);
    }

    public Optional<PartnerResponse> getById(Long id) {
        return partnerRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .map(partnerMapper::toResponse);
    }

    @Transactional
    public PartnerResponse create(PartnerRequest request) {
        Partner entity = partnerMapper.toEntity(request);
        return partnerMapper.toResponse(partnerRepository.save(entity));
    }

    @Transactional
    public PartnerResponse update(Long id, PartnerRequest request) {
        Partner entity = partnerMapper.toEntity(request);
        entity.setId(id);
        return partnerMapper.toResponse(partnerRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        partnerRepository.findById(id).ifPresent(partner -> {
            partner.setDeletedAt(java.time.LocalDateTime.now());
            partnerRepository.save(partner);
        });
    }

    private PageResponse<PartnerResponse> toPageResponse(Page<Partner> page) {
        List<PartnerResponse> content = page.getContent().stream()
                .map(partnerMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
