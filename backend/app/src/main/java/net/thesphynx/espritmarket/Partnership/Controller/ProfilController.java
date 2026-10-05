package net.thesphynx.espritmarket.Partnership.Controller;

import lombok.RequiredArgsConstructor;
import net.thesphynx.espritmarket.Partnership.Dto.ProfilRequest;
import net.thesphynx.espritmarket.Partnership.Entity.Profil;
import net.thesphynx.espritmarket.Partnership.Service.ProfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/partnership/profil")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ProfilController {

    private final ProfilService profilService;

    @GetMapping("/{studentId}")
    public ResponseEntity<Profil> getProfil(@PathVariable Long studentId) {
        return profilService.getByStudentId(studentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{studentId}")
    public ResponseEntity<Profil> saveOrUpdateProfil(
            @PathVariable Long studentId,
            @RequestBody ProfilRequest request) {
        Profil updated = profilService.saveOrUpdate(studentId, request);
        return ResponseEntity.ok(updated);
    }
}
