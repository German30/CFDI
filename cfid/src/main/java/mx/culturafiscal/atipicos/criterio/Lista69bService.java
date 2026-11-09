package mx.culturafiscal.atipicos.criterio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import mx.culturafiscal.atipicos.common.exception.ReglaNegocioException;
import mx.culturafiscal.atipicos.security.SecurityUtils;

/** Importa el CSV del SAT; busca la fila de encabezado porque el archivo trae líneas previas. */
@Service
@RequiredArgsConstructor
public class Lista69bService {
    private final Lista69bRepository repo;

    @Transactional
    public int importar(MultipartFile archivo) {
        try {
            return importar(SecurityUtils.usuarioActual().getId(), archivo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Transactional
    public int importar(Long usuarioId, byte[] bytes) {
        String[] lineas = decodificar(bytes).replace("﻿", "").split("\\r?\\n");
        int iRfc = -1, iNombre = -1, iSit = -1, inicio = -1;
        for (int i = 0; i < lineas.length && inicio < 0; i++) {
            List<String> h = campos(lineas[i]);
            for (int j = 0; j < h.size(); j++) {
                String s = h.get(j).trim().toUpperCase(Locale.ROOT);
                if (s.equals("RFC")) iRfc = j;
                else if (s.startsWith("NOMBRE")) iNombre = j;
                else if (s.startsWith("SITUACI")) iSit = j;
            }
            if (iRfc >= 0 && iSit >= 0) inicio = i + 1;
            else { iRfc = -1; iNombre = -1; iSit = -1; }
        }
        if (inicio < 0) throw new ReglaNegocioException("El CSV no contiene las columnas RFC y Situación");
        Map<String, Lista69b> r = new LinkedHashMap<>();
        for (int i = inicio; i < lineas.length; i++) {
            List<String> v = campos(lineas[i]);
            if (v.size() <= Math.max(iRfc, iSit)) continue;
            String rfc = v.get(iRfc).trim().toUpperCase(Locale.ROOT);
            if (rfc.length() < 12 || rfc.length() > 13) continue;
            Lista69b l = new Lista69b();
            l.setUsuarioId(usuarioId);
            l.setRfc(rfc);
            if (iNombre >= 0 && iNombre < v.size()) l.setNombre(corta(v.get(iNombre).trim(), 300));
            l.setSituacion(corta(v.get(iSit).trim(), 60));
            r.put(rfc, l);
        }
        repo.borrar(usuarioId);
        repo.saveAll(r.values());
        return r.size();
    }

    private static String corta(String s, int n) { return s.length() <= n ? s : s.substring(0, n); }

    private static String decodificar(byte[] b) {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(b)).toString();
        } catch (CharacterCodingException e) {
            return new String(b, Charset.forName("windows-1252"));
        }
    }

    /** Separa una línea CSV respetando comillas dobles. */
    static List<String> campos(String l) {
        List<String> r = new ArrayList<>();
        StringBuilder s = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < l.length(); i++) {
            char ch = l.charAt(i);
            if (ch == '"') {
                if (q && i + 1 < l.length() && l.charAt(i + 1) == '"') { s.append('"'); i++; }
                else q = !q;
            } else if (ch == ',' && !q) {
                r.add(s.toString());
                s.setLength(0);
            } else {
                s.append(ch);
            }
        }
        r.add(s.toString());
        return r;
    }
}
