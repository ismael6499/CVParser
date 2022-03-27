
package servlets;

import datasets.DataFinder;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.util.TextUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.json.JSONObject;
import utils.EmailFinder;
import utils.PhoneFinder;
import utils.TextUtilCustom;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet(name = "UploadFile", value = "/UploadFile")
public class UploadFile extends HttpServlet {

    private DataFinder dataFinder;

    private String apellidoSaved;
    private String nombreSaved;
    private String nombreSegundoSaved;
    private String telefonoSaved;
    private String emailSaved;

    @Override
    protected synchronized void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected synchronized void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "GET");
            response.addHeader("Access-Control-Allow-Methods", "POST");
            response.addHeader("Access-Control-Allow-Headers", "Origin");
            response.addHeader("Access-Control-Allow-Headers", "Content-Type");
            response.addHeader("Access-Control-Allow-Headers", "X-Auth-Token");
            response.addHeader("Access-Control-Max-Age", "86400");
            resetCampos();
            procesar(request, response);
        } catch (IOException | ServletException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println(e);
        }
    }

    private void resetCampos() {
        apellidoSaved = null;
        nombreSaved = null;
        nombreSegundoSaved = null;
        telefonoSaved = null;
        emailSaved = null;
    }

    private void procesar(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String resultFromFile = getTextFromFile(request, response);
        if (TextUtils.isBlank(resultFromFile)) {
            throw new ServletException("No se ha podido analizar el texto del archivo");
        }
        dataFinder = DataFinder.getInstance();

        HashSet<String> palabrasResultado = new HashSet<>();
        String[] lineas = getLineasFromResult(resultFromFile);
        lineas = ordenarLineasPorTags(lineas);
        analizarLineas(palabrasResultado, lineas);

        JSONObject jsonObject = new JSONObject();
        if (!TextUtils.isBlank(apellidoSaved)) {
            jsonObject.put("apellido", apellidoSaved);
        }
        if (!TextUtils.isBlank(nombreSaved)) {
            jsonObject.put("nombre", nombreSaved);
        }
        if (!TextUtils.isBlank(nombreSegundoSaved)) {
            jsonObject.put("nombreSegundo", nombreSegundoSaved);
        }
        if (!TextUtils.isBlank(telefonoSaved)) {
            jsonObject.put("telefono", telefonoSaved);
        }
        if (!TextUtils.isBlank(emailSaved)) {
            jsonObject.put("email", emailSaved);
        }
        response.getWriter().println(jsonObject);
    }

    private String[] ordenarLineasPorTags(String[] lineas) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            if(linea.toLowerCase().contains("nombre") || linea.toLowerCase().contains("apellido")){
                indexes.add(i);
            }
        }
        LinkedHashSet<String> lineasOrdenadas = new LinkedHashSet<>();
        for (Integer index : indexes) {
            String linea = lineas[index];
            lineasOrdenadas.add(linea);
        }
        lineasOrdenadas.addAll(Arrays.asList(lineas));
        return lineasOrdenadas.toArray(new String[0]);
    }


    private void analizarLineas(HashSet<String> palabrasResultado, String[] lineas) {
        for (String linea : lineas) {
            String[] palabras = linea.split("[^\\wÀ-úÀ-ÿ]");
            for (String palabra : palabras) {
                if (variablesCompletas()) return;
                if (!TextUtils.isBlank(palabra) && palabra.length() > 3) {
                    palabra = removeTextSymbol(palabra);
                    if (!palabrasResultado.contains(palabra.toUpperCase())) {
                        palabrasResultado.add(palabra.toUpperCase());
                        if (buscarNombres(palabras, palabra)) continue;

                        if (apellidoSaved == null) {
                            if (buscarApellido(palabra)) {
                                continue;
                            }
                        }

                        if (emailSaved == null) {
                            if (buscarEmail(palabra)) {
                                continue;
                            }
                        }

                        if (telefonoSaved == null && !palabra.replaceAll("\\D", "").isEmpty()) {
                            buscarTelefono(Arrays.stream(palabras).reduce("", (s, s2) -> s + s2).replaceAll("\\D", ""));
                        }
                    }

                }
            }
        }
    }

    private boolean buscarNombres(String[] palabras, String palabra) {
        List<String> lista = Arrays.stream(palabras).collect(Collectors.toList());
        int index = lista.indexOf(palabra);
        if (index > 1) {
            lista = lista.subList(0, index);
            for (String text : lista) {
                if (text.toUpperCase().contains("APELLIDO")) {
                    return false;
                }
            }
        }
        if (nombreSaved == null) {
            if (buscarNombre(palabra) && !palabra.equals(apellidoSaved)) {
                for (String nombreSegundo : palabras) {
                    if (!nombreSegundo.equals(palabra)) {
                        buscarApellido(palabra);
                        if (nombreSegundoSaved == null && !palabra.equals(apellidoSaved)) {
                            buscarSegundoNombre(nombreSegundo);
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    private String removeTextSymbol(String palabra) {
        return palabra.replaceAll("[\\r\\n\\t]", "");
    }

    private boolean variablesCompletas() {
        return apellidoSaved != null && nombreSaved != null && nombreSegundoSaved != null && emailSaved != null && telefonoSaved != null;
    }

    private void buscarTelefono(String palabra) {
        boolean validate = PhoneFinder.validate(palabra);
        if (validate) {
            telefonoSaved = palabra;
        }
    }

    private boolean buscarEmail(String palabra) {
        boolean validate = EmailFinder.validate(palabra);
        if (validate) {
            emailSaved = palabra;
        }
        return validate;
    }

    private void buscarSegundoNombre(String palabra) {
        if (dataFinder.existeNombre(palabra.toUpperCase())) {
            String nombreFormateado = TextUtilCustom.formatToName(palabra);
            if (!nombreFormateado.equals(apellidoSaved)) {
                this.nombreSegundoSaved = nombreFormateado;
            }
        }
    }

    private boolean buscarNombre(String palabra) {
        if (dataFinder.existeNombre(palabra.toUpperCase())) {
            String nombreFormateado = TextUtilCustom.formatToName(palabra);
            if (!nombreFormateado.equals(apellidoSaved)) {
                this.nombreSaved = nombreFormateado;
            }
        }
        return nombreSaved != null && !nombreSaved.trim().isEmpty();
    }

    private boolean buscarApellido(String palabra) {
        if (apellidoSaved == null) {
            if (dataFinder.existeApellido(palabra.toUpperCase())) {
                String apellidoFormateado = TextUtilCustom.formatToName(palabra);
                if (!apellidoFormateado.equals(nombreSaved) && !apellidoFormateado.equals(nombreSegundoSaved)) {
                    this.apellidoSaved = apellidoFormateado;
                }
            }
        }
        return apellidoSaved != null && !apellidoSaved.trim().isEmpty();

    }

    private String[] getLineasFromResult(String resultFromFile) {
        return resultFromFile.split("[\n\r]");
    }

    private String getTextFromFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload fileUpload = new ServletFileUpload(factory);
        if (!ServletFileUpload.isMultipartContent(request)) {
            throw new IOException("error multipart request not found");
        }

        String result = "";
        try {
            List<FileItem> items = fileUpload.parseRequest(request);
            if (items == null) {
                response.getWriter().write("File not correctly uploaded");
                return null;
            }
            Iterator<FileItem> iter = items.iterator();

            if (iter.hasNext()) {
                FileItem item = iter.next();
                response.setHeader("Content-Type", "text/html");
                BodyContentHandler handler = new BodyContentHandler();

                AutoDetectParser parser = new AutoDetectParser();
                Metadata metadata = new Metadata();
                try (InputStream stream = item.getInputStream()) {
                    parser.parse(stream, handler, metadata);
                    result = handler.toString();
                }
            }
        } catch (Exception ex) {
        }
        return result;
    }
}

