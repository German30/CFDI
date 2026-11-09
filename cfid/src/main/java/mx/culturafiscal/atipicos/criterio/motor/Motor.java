package mx.culturafiscal.atipicos.criterio.motor;

import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.culturafiscal.atipicos.criterio.CriterioService;
import mx.culturafiscal.atipicos.criterio.CriterioService.Efectivo;

@Slf4j
@Component
@RequiredArgsConstructor
public class Motor {
    private final CatalogoCriterios catalogo;
    private final CriterioService criterios;

    public void ejecutar(Contexto x) {
        Map<String, Efectivo> cfg = criterios.efectivos(x.contribuyente.getId());
        for (Criterio c : catalogo.todos()) {
            Efectivo e = cfg.get(c.codigo());
            if (!e.activo()) continue;
            x.iniciar(c.codigo());
            c.detector().evaluar(x, new Params(e.parametros()));
        }
    }
}
