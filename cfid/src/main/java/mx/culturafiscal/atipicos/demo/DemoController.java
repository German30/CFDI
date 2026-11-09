package mx.culturafiscal.atipicos.demo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.demo.DemoService.Cargado;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {
    private final DemoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<Cargado> cargar(@RequestParam(defaultValue = "2024") int ejercicio, @RequestParam(defaultValue = "600") int operacionesBase) {
        return service.cargar(ejercicio, Math.min(Math.max(operacionesBase, 240), 20000));
    }
}
