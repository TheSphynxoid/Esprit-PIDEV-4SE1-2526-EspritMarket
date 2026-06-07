package net.thesphynx.espritmarket.EventPlanning.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.thesphynx.espritmarket.EventPlanning.Service.EventSchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for scheduler management
 * Provides endpoints to manually trigger and monitor scheduler tasks
 */
@RestController
@RequestMapping("/api/eventplanning/scheduler")
@RequiredArgsConstructor
@Tag(name = "EventPlanning - Scheduler", description = "Scheduler management endpoints")
public class SchedulerController {

    private final EventSchedulerService schedulerService;

    /**
     * Manually trigger the scheduler to run immediately
     * Useful for testing or forcing an immediate update
     * 
     * Example: curl -X POST http://localhost:8088/api/eventplanning/scheduler/trigger
     */
    @PostMapping("/trigger")
    @Operation(summary = "Trigger scheduler manually", 
        description = "Forces the scheduler to run immediately (for testing)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scheduler triggered successfully"),
        @ApiResponse(responseCode = "500", description = "Error occurred during execution")
    })
    public ResponseEntity<String> triggerScheduler() {
        try {
            schedulerService.triggerNow();
            return ResponseEntity.ok("✓ Scheduler executed successfully. Check application logs for details.");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("✗ Error executing scheduler: " + e.getMessage());
        }
    }
}
