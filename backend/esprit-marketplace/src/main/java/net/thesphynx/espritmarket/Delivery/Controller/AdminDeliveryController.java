package net.thesphynx.espritmarket.Delivery.Controller;

import net.thesphynx.espritmarket.Delivery.Dto.AdminDeliveryOverviewResponse;
import net.thesphynx.espritmarket.Delivery.Service.AdminDeliveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/delivery")
public class AdminDeliveryController {

    private final AdminDeliveryService adminDeliveryService;

    public AdminDeliveryController(AdminDeliveryService adminDeliveryService) {
        this.adminDeliveryService = adminDeliveryService;
    }

    @GetMapping("/overview")
    public AdminDeliveryOverviewResponse getOverview(@RequestParam(name = "days", defaultValue = "7") int days) {
        return adminDeliveryService.getOverview(days);
    }
}
