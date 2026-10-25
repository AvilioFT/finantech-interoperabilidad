package com.finantech.transacciones.controller;

import com.finantech.transacciones.dto.ErrorResponse;
import com.finantech.transacciones.dto.TransaccionRequest;
import com.finantech.transacciones.dto.TransaccionResponse;
import com.finantech.transacciones.service.TransaccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transacciones")
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @PostMapping
    public ResponseEntity<TransaccionResponse> create(@Valid @RequestBody TransaccionRequest request,
                                                      @RequestHeader("Idempotency-Key") String idempotencyKey) {
        TransaccionResponse response = transaccionService.crearTransaccion(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        TransaccionResponse r = transaccionService.obtenerTransaccion(id);
        if (r == null) {
            ErrorResponse err = ErrorResponse.of(404, "NOT-001", "Transaccion no encontrada",
                    "/api/v1/transacciones/" + id, java.util.UUID.randomUUID().toString());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
        }
        return ResponseEntity.ok(r);
    }
}
