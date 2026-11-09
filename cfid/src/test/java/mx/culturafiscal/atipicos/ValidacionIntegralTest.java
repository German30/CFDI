package mx.culturafiscal.atipicos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import mx.culturafiscal.atipicos.cfdi.CfdiRepository;
import mx.culturafiscal.atipicos.cfdi.CfdiXmlParser;
import mx.culturafiscal.atipicos.common.exception.CfdiInvalidoException;
import mx.culturafiscal.atipicos.common.exception.ReglaNegocioException;
import mx.culturafiscal.atipicos.conciliacion.ConciliacionDtos.Tablero;
import mx.culturafiscal.atipicos.conciliacion.ConciliacionService;
import mx.culturafiscal.atipicos.contribuyente.Contribuyente;
import mx.culturafiscal.atipicos.contribuyente.ContribuyenteRepository;
import mx.culturafiscal.atipicos.criterio.motor.CatalogoCriterios;
import mx.culturafiscal.atipicos.criterio.motor.Criterio;
import mx.culturafiscal.atipicos.demo.DemoGenerador;
import mx.culturafiscal.atipicos.demo.DemoGenerador.Archivo;
import mx.culturafiscal.atipicos.demo.DemoGenerador.Conjunto;
import mx.culturafiscal.atipicos.demo.DemoGenerador.Demo;
import mx.culturafiscal.atipicos.demo.DemoService;
import mx.culturafiscal.atipicos.demo.DemoService.Cargado;
import mx.culturafiscal.atipicos.exportacion.ExportacionService;
import mx.culturafiscal.atipicos.hallazgo.AnalisisService;
import mx.culturafiscal.atipicos.hallazgo.EstatusHallazgo;
import mx.culturafiscal.atipicos.hallazgo.Hallazgo;
import mx.culturafiscal.atipicos.hallazgo.HallazgoDtos.CambioEstatus;
import mx.culturafiscal.atipicos.malla.MallaDtos.Celda;
import mx.culturafiscal.atipicos.malla.MallaDtos.Fila;
import mx.culturafiscal.atipicos.malla.MallaDtos.Filtros;
import mx.culturafiscal.atipicos.malla.MallaDtos.Malla;
import mx.culturafiscal.atipicos.malla.MallaService;
import mx.culturafiscal.atipicos.respaldo.RespaldoService;
import mx.culturafiscal.atipicos.resumen.ResumenEjecutivo;
import mx.culturafiscal.atipicos.resumen.ResumenService;
import mx.culturafiscal.atipicos.usuario.Rol;
import mx.culturafiscal.atipicos.usuario.Usuario;
import mx.culturafiscal.atipicos.usuario.UsuarioRepository;

/** Validación del apartado 12 de la especificación, sobre el flujo real con datos generados. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidacionIntegralTest {
    private static final int EJ = 2024;
    private static final Filtros SIN = new Filtros(null, null, null, null, null);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ContribuyenteRepository contribuyentes;
    @Autowired CfdiRepository cfdis;
    @Autowired DemoService demo;
    @Autowired AnalisisService analisis;
    @Autowired MallaService malla;
    @Autowired ConciliacionService conciliacion;
    @Autowired ResumenService resumen;
    @Autowired ExportacionService exportacion;
    @Autowired RespaldoService respaldo;
    @Autowired CatalogoCriterios catalogo;
    @Autowired CfdiXmlParser parser;

    Demo datos;
    List<Cargado> cargados;

    @BeforeAll
    void cargar() {
        Usuario u = usuarios.save(Usuario.builder().nombre("Prueba").email("demo@prueba.mx").password("x").rol(Rol.USUARIO).activo(true).build());
        datos = DemoGenerador.generar(EJ, 600, 7L);
        cargados = demo.cargar(u.getId(), datos);
    }

    private Contribuyente c(int i) { return contribuyentes.findById(cargados.get(i).contribuyenteId()).orElseThrow(); }

    @Test
    void cadaCriterioDetectaSuCasoSembrado() {
        Set<String> detectados = new TreeSet<>();
        for (Cargado k : cargados) analisis.listar(k.contribuyenteId(), EJ).forEach(h -> detectados.add(h.getCodigoCriterio()));
        Set<String> catalogados = catalogo.todos().stream().map(Criterio::codigo).collect(Collectors.toCollection(TreeSet::new));
        Set<String> faltan = new TreeSet<>(catalogados);
        faltan.removeAll(detectados);
        assertThat(faltan).as("criterios sin detección").isEmpty();
    }

    @Test
    void todoCriterioTieneFundamentoYRecomendacion() {
        assertThat(catalogo.todos()).allSatisfy(k -> {
            assertThat(k.fundamento()).isNotBlank();
            assertThat(k.consecuencia()).isNotBlank();
            assertThat(k.recomendacion()).isNotBlank();
        });
    }

    @Test
    void totalesDeLaMallaCoincidenConLaSumaDeCfdi() {
        for (int i = 0; i < cargados.size(); i++) verificarMalla(c(i), datos.conjuntos().get(i));
    }

    private void verificarMalla(Contribuyente k, Conjunto s) {
        Malla m = malla.construir(k, EJ, SIN);
        Map<String, BigDecimal> real = new HashMap<>();
        for (Fila f : m.filas()) {
            for (Celda x : f.celdas()) {
                if (x.operaciones() > 0) real.put(f.tipo() + "|" + f.direccion() + "|" + x.mes(), x.importe());
            }
            BigDecimal suma = f.celdas().stream().map(Celda::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(f.totalImporte()).isEqualByComparingTo(suma);
        }
        assertThat(real.keySet()).isEqualTo(s.esperado().keySet());
        s.esperado().forEach((key, v) -> assertThat(real.get(key)).as(key).isEqualByComparingTo(v));
    }

    @Test
    void cruceDeCanceladosYConciliacion() {
        Tablero t = conciliacion.conciliar(c(0), EJ).tablero();
        assertThat(t.cancelados()).isEqualTo(datos.conjuntos().get(0).canceladosMetadata()).isEqualTo(6);
        assertThat(t.faltantes()).isEqualTo(5);
        assertThat(t.sinMetadata()).isEqualTo(3);
        assertThat(t.diferenciasMonto()).isEqualTo(1);
        assertThat(t.canceladosFueraEjercicio()).isEqualTo(2);
        // Persona moral: el plazo vence el 31 de marzo siguiente; solo la de junio lo rebasa.
        assertThat(t.canceladosFueraPlazo()).isEqualTo(1);
        assertThat(t.canceladosConPagoOEgreso()).isEqualTo(1);
        long enMalla = malla.construir(c(0), EJ, SIN).filas().stream().mapToLong(Fila::totalCancelados).sum();
        assertThat(enMalla).as("incluye el cancelado sin XML").isEqualTo(6);
    }

    @Test
    void limiteDeCancelacionDistingueMoralYFisica() {
        assertThat(ConciliacionService.limiteCancelacion("ABC010101AB1", 2024).toLocalDate()).hasToString("2025-03-31");
        assertThat(ConciliacionService.limiteCancelacion("ABCD010101AB1", 2024).toLocalDate()).hasToString("2025-04-30");
    }

    @Test
    void reanalisisConservaEstatusDelHallazgo() {
        Contribuyente k = c(1);
        Hallazgo h = analisis.listar(k.getId(), EJ).get(0);
        analisis.cambiarEstatus(k.getId(), h.getId(), new CambioEstatus(EstatusHallazgo.ACLARADO, "Soportado"));
        analisis.analizar(k, EJ);
        Hallazgo otra = analisis.listar(k.getId(), EJ).stream().filter(x -> x.getHuella().equals(h.getHuella())).findFirst().orElseThrow();
        assertThat(otra.getEstatus()).isEqualTo(EstatusHallazgo.ACLARADO);
        assertThat(otra.getNotas()).isEqualTo("Soportado");
    }

    @Test
    void matrizUbicaCadaHallazgo() {
        var m = analisis.matriz(c(0), EJ);
        assertThat(m.celdas()).hasSize(25);
        assertThat(m.celdas().stream().mapToLong(x -> x.hallazgos()).sum()).isEqualTo(analisis.listar(c(0).getId(), EJ).size());
    }

    @Test
    void resumenYExportaciones() throws Exception {
        Contribuyente k = c(0);
        ResumenEjecutivo r = resumen.generar(k, EJ);
        assertThat(r.getTexto()).contains("# Alcance y metodología", "# Limitaciones del análisis", "Se identificaron", k.getRfc());
        resumen.editar(k.getId(), EJ, r.getTexto() + "\nNota del revisor.");
        byte[] pdf = exportacion.pdf(k, EJ);
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(exportacion.excel(k, EJ)))) {
            assertThat(wb.getSheet("Malla").getLastRowNum()).isGreaterThan(5);
            assertThat(wb.getSheet("Hallazgos").getLastRowNum()).isGreaterThan(5);
            assertThat(wb.getSheet("Matriz").getLastRowNum()).isEqualTo(25);
            assertThat(wb.getSheet("Cancelados").getLastRowNum()).isEqualTo(6);
            assertThat(wb.getSheet("Faltantes").getLastRowNum()).isEqualTo(5);
        }
    }

    @Test
    void respaldoYRestauracionDevuelvenElMismoEstado() {
        Contribuyente k = c(2);
        Malla antes = malla.construir(k, EJ, SIN);
        int hallazgos = analisis.listar(k.getId(), EJ).size();
        long n = cfdis.countByContribuyenteId(k.getId());
        byte[] r = respaldo.exportarJson(k);
        Long id = k.getId();
        cfdis.deleteAll(cfdis.findByContribuyenteId(id).subList(0, 10));
        assertThat(cfdis.countByContribuyenteId(id)).isEqualTo(n - 10);
        respaldo.restaurar(k, r);
        assertThat(cfdis.countByContribuyenteId(id)).isEqualTo(n);
        assertThat(analisis.listar(id, EJ)).hasSize(hallazgos);
        assertThat(json.valueToTree(malla.construir(k, EJ, SIN)).toString()).isEqualTo(json.valueToTree(antes).toString());
    }

    @Test
    void restauracionRechazaArchivosInvalidos() {
        Contribuyente k = c(2);
        long n = cfdis.countByContribuyenteId(k.getId());
        assertThatThrownBy(() -> respaldo.restaurar(k, "no es json".getBytes())).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> respaldo.restaurar(k, "{\"formato\":\"otro\"}".getBytes())).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> respaldo.restaurar(k, "{\"formato\":\"cfdi-atipicos-respaldo\",\"version\":99}".getBytes())).hasMessageContaining("Versión");
        assertThatThrownBy(() -> respaldo.restaurar(k, respaldo.exportarJson(c(1)))).hasMessageContaining("pertenece al RFC");
        assertThat(cfdis.countByContribuyenteId(k.getId())).isEqualTo(n);
    }

    @Test
    void respaldoAutomaticoRotaYOmiteSinCambios() {
        Contribuyente k = c(1);
        for (int i = 0; i < 12; i++) respaldo.crearInstantanea(k, "MANUAL");
        assertThat(respaldo.listar(k.getId())).hasSize(10);
        contribuyentes.marcarCambio(k.getId(), java.time.LocalDateTime.now());
        respaldo.respaldoAutomatico();
        var l = respaldo.listar(k.getId());
        assertThat(l).hasSize(10);
        assertThat(l.get(0).getOrigen()).isEqualTo("AUTOMATICO");
        Long ultimo = l.get(0).getId();
        respaldo.respaldoAutomatico();
        assertThat(respaldo.listar(k.getId()).get(0).getId()).as("sin cambios no hay versión nueva").isEqualTo(ultimo);
    }

    @Test
    void parserRechazaXxeYArchivosInvalidos() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE c [<!ENTITY x SYSTEM \"file:///c:/windows/win.ini\">]><Comprobante Total=\"&x;\"/>";
        assertThatThrownBy(() -> parser.parsear(xxe.getBytes())).isInstanceOf(CfdiInvalidoException.class);
        assertThatThrownBy(() -> parser.parsear("<a/>".getBytes())).isInstanceOf(CfdiInvalidoException.class);
        assertThatThrownBy(() -> parser.parsear("no xml".getBytes())).isInstanceOf(CfdiInvalidoException.class);
        assertThatThrownBy(() -> parser.parsear("<cfdi:Comprobante xmlns:cfdi=\"x\" Total=\"1\"/>".getBytes())).hasMessageContaining("TimbreFiscalDigital");
    }

    @Test
    void parserExtraeCamposDeComplementosYIgnoraAddenda() {
        String xml = new String(datos.conjuntos().get(2).xml().stream().map(Archivo::contenido)
                .filter(b -> new String(b, StandardCharsets.UTF_8).contains("nomina12:Nomina")).findFirst().orElseThrow(), StandardCharsets.UTF_8);
        String conAddenda = xml.replace("</cfdi:Comprobante>", "<cfdi:Addenda><Concepto ClaveProdServ=\"99\"/></cfdi:Addenda></cfdi:Comprobante>");
        var c = parser.parsear(conAddenda.getBytes(StandardCharsets.UTF_8));
        assertThat(c.getTipoNomina()).isEqualTo("O");
        assertThat(c.getCurpTrabajador()).isNotBlank();
        assertThat(c.getPeriodicidadPago()).isEqualTo("05");
        assertThat(c.getConceptos()).hasSize(1);
    }

    // ---- API ----

    private String registrar(String email) throws Exception {
        String r = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre", "Revisor", "email", email, "password", "ClaveSegura123"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(r).get("accessToken").asText();
    }

    private Long alta(String token, String rfc) throws Exception {
        String r = mvc.perform(post("/api/contribuyentes").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("rfc", rfc, "razonSocial", "Ficticia SA", "ejercicio", EJ))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("id").asLong();
    }

    @Test
    void apiExigeTokenYAislaUsuarios() throws Exception {
        mvc.perform(get("/api/contribuyentes")).andExpect(status().isUnauthorized());
        String a = registrar("a@prueba.mx"), b = registrar("b@prueba.mx");
        Long id = alta(a, "AAA010101AA1");
        mvc.perform(get("/api/contribuyentes/" + id).header("Authorization", a)).andExpect(status().isOk());
        for (String ruta : List.of("", "/malla", "/conciliacion", "/hallazgos", "/matriz", "/criterios", "/respaldos", "/respaldos/descarga")) {
            mvc.perform(get("/api/contribuyentes/" + id + ruta).header("Authorization", b)).andExpect(status().isNotFound());
        }
        mvc.perform(delete("/api/contribuyentes/" + id).header("Authorization", b)).andExpect(status().isNotFound());
    }

    @Test
    void refreshNoSirveComoAccessYLoginFalla401() throws Exception {
        registrar("r@prueba.mx");
        String l = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", "r@prueba.mx", "password", "ClaveSegura123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode n = json.readTree(l);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + n.get("refreshToken").asText())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", n.get("refreshToken").asText())))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", "r@prueba.mx", "password", "incorrecta")))).andExpect(status().isUnauthorized());
    }

    @Test
    void eliminacionTotalExigeDobleConfirmacion() throws Exception {
        String t = registrar("borra@prueba.mx");
        Long id = alta(t, "BBB010101BB2");
        String s = mvc.perform(post("/api/datos/eliminacion").header("Authorization", t)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(s).get("token").asText();
        mvc.perform(delete("/api/datos/eliminacion").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", token, "frase", "eliminar")))).andExpect(status().isUnprocessableEntity());
        assertThat(contribuyentes.existsById(id)).isTrue();
        s = mvc.perform(post("/api/datos/eliminacion").header("Authorization", t)).andReturn().getResponse().getContentAsString();
        mvc.perform(delete("/api/datos/eliminacion").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token", json.readTree(s).get("token").asText(), "frase", "ELIMINAR TODO")))).andExpect(status().isNoContent());
        assertThat(contribuyentes.existsById(id)).isFalse();
    }

    @Test
    void cargaDeCincoMilXmlNoBloqueaYCuadra() throws Exception {
        Demo d = DemoGenerador.generar(EJ, 5000, 99L);
        Conjunto s = d.conjuntos().get(0);
        assertThat(s.xml().size()).isGreaterThanOrEqualTo(5000);
        String t = registrar("masivo@prueba.mx");
        Long id = alta(t, s.contribuyente().rfc());

        ByteArrayOutputStream zip = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(zip)) {
            for (Archivo a : s.xml()) {
                z.putNextEntry(new ZipEntry(a.nombre()));
                z.write(a.contenido());
                z.closeEntry();
            }
            z.putNextEntry(new ZipEntry("roto.xml"));
            z.write("<no".getBytes());
            z.closeEntry();
            z.putNextEntry(new ZipEntry("duplicado.xml"));
            z.write(s.xml().get(0).contenido());
            z.closeEntry();
        }
        JsonNode meta = subir(t, id, "metadata", new MockMultipartFile("archivos", "metadata.txt", "text/plain", s.metadata()));
        assertThat(esperar(t, id, meta.get("id").asLong()).get("estado").asText()).isEqualTo("COMPLETADO");

        long inicio = System.currentTimeMillis();
        JsonNode job = subir(t, id, "xml", new MockMultipartFile("archivos", "cfdi.zip", "application/zip", zip.toByteArray()));
        long respuesta = System.currentTimeMillis() - inicio;
        assertThat(respuesta).as("la petición responde sin esperar el procesamiento").isLessThan(3000);

        JsonNode fin = esperar(t, id, job.get("id").asLong());
        long total = System.currentTimeMillis() - inicio;
        System.out.printf("CARGA MASIVA: %d XML, respuesta %d ms, procesados en %d ms%n", s.xml().size(), respuesta, total);
        assertThat(fin.get("estado").asText()).isEqualTo("COMPLETADO");
        assertThat(fin.get("importados").asInt()).isEqualTo(s.xml().size());
        assertThat(fin.get("duplicados").asInt()).isEqualTo(1);
        assertThat(fin.get("errores").asInt()).isEqualTo(1);
        String err = mvc.perform(get("/api/contribuyentes/" + id + "/importaciones/" + job.get("id").asLong() + "/errores").header("Authorization", t))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(err).contains("roto.xml", "duplicado.xml");
        verificarMalla(contribuyentes.findById(id).orElseThrow(), s);
    }

    private JsonNode subir(String t, Long id, String tipo, MockMultipartFile f) throws Exception {
        String r = mvc.perform(multipart("/api/contribuyentes/" + id + "/importaciones/" + tipo).file(f).header("Authorization", t))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        return json.readTree(r);
    }

    private JsonNode esperar(String t, Long id, long job) throws Exception {
        long limite = System.currentTimeMillis() + 180_000;
        while (System.currentTimeMillis() < limite) {
            JsonNode n = json.readTree(mvc.perform(get("/api/contribuyentes/" + id + "/importaciones/" + job).header("Authorization", t))
                    .andReturn().getResponse().getContentAsString());
            String e = n.get("estado").asText();
            if (e.equals("COMPLETADO") || e.equals("FALLIDO")) return n;
            Thread.sleep(200);
        }
        throw new AssertionError("La importación no terminó a tiempo");
    }
}
