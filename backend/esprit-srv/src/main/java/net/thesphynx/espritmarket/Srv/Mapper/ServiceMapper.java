package net.thesphynx.espritmarket.Srv.Mapper;

import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Srv.Dto.*;
import net.thesphynx.espritmarket.Srv.Entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ServiceMapper {

    public Service toEntity(ServiceUpsertRequest request) {
        if (request == null) return null;
        Service service = new Service();
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setCategory(request.getCategory());
        service.setPricingType(request.getPricingType());
        if (request.getPrice() != null) {
            service.setPrice(request.getPrice());
        }
        service.setStatus(request.getStatus());
        service.setRating(request.getRating());
        service.setLocation(request.getLocation());
        service.setImageUrl(request.getImageUrl());
        service.setAllowProjectParticipation(request.getAllowProjectParticipation() == null || request.getAllowProjectParticipation());

        User provider = new User();
        provider.setId(request.getProviderId());
        service.setProvider(provider);

        if (request.getPackages() != null) {
            for (int i = 0; i < request.getPackages().size(); i++) {
                ServicePackageRequest pkg = request.getPackages().get(i);
                service.getPackages().add(toPackageEntity(service, pkg, i));
            }
        }

        if (service.getPrice() == null && service.getPackages() != null && !service.getPackages().isEmpty()) {
            service.setPrice(service.getPackages().stream()
                    .map(ServicePackage::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO));
        }

        return service;
    }

    public ServiceResponse toResponse(Service service) {
        if (service == null) return null;
        ServiceResponse response = new ServiceResponse();
        response.setId(service.getId());
        response.setName(service.getName());
        response.setDescription(service.getDescription());
        response.setCategory(service.getCategory());
        response.setPricingType(service.getPricingType());
        response.setPrice(service.getPrice());
        response.setStatus(service.getStatus());
        response.setRating(service.getRating());
        response.setLocation(service.getLocation());
        response.setImageUrl(service.getImageUrl());
        response.setAllowProjectParticipation(service.isAllowProjectParticipation());
        if (service.getProvider() != null) {
            response.setProviderId(service.getProvider().getId());
            response.setProviderName(service.getProvider().getName());
        }
        if (service.getTags() != null) {
            response.setTags(service.getTags().stream()
                    .map(ServiceTag::getTag)
                    .collect(Collectors.toList()));
        }
        if (service.getPackages() != null && !service.getPackages().isEmpty()) {
            List<ServicePackageResponse> pkgResponses = service.getPackages().stream()
                    .map(this::toPackageResponse)
                    .collect(Collectors.toList());
            response.setPackages(pkgResponses);
            response.setStartingPrice(pkgResponses.stream()
                    .map(ServicePackageResponse::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(service.getPrice()));
        } else {
            response.setStartingPrice(service.getPrice());
        }
        return response;
    }

    public List<ServiceResponse> toResponseList(List<Service> services) {
        return services.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ServicePackage toPackageEntity(Service service, ServicePackageRequest request, int sortOrder) {
        ServicePackage pkg = new ServicePackage();
        pkg.setService(service);
        pkg.setTier(PackageTier.valueOf(request.getTier().toUpperCase()));
        pkg.setName(request.getName());
        pkg.setDescription(request.getDescription());
        pkg.setPrice(request.getPrice());
        pkg.setDeliveryDays(request.getDeliveryDays() != null ? request.getDeliveryDays() : 7);
        pkg.setRevisions(request.getRevisions() != null ? request.getRevisions() : 1);
        pkg.setSortOrder(sortOrder);
        if (request.getFeatures() != null) {
            pkg.setFeatures(String.join("|||", request.getFeatures()));
        }
        return pkg;
    }

    private ServicePackageResponse toPackageResponse(ServicePackage pkg) {
        ServicePackageResponse resp = new ServicePackageResponse();
        resp.setId(pkg.getId());
        resp.setTier(pkg.getTier().name());
        resp.setName(pkg.getName());
        resp.setDescription(pkg.getDescription());
        resp.setPrice(pkg.getPrice());
        resp.setDeliveryDays(pkg.getDeliveryDays());
        resp.setRevisions(pkg.getRevisions());
        if (pkg.getFeatures() != null && !pkg.getFeatures().isBlank()) {
            resp.setFeatures(Arrays.asList(pkg.getFeatures().split("\\|\\|\\|")));
        } else {
            resp.setFeatures(List.of());
        }
        return resp;
    }
}
