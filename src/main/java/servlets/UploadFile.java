
package servlets;

import datasets.DataFinder;
import datasets.NameObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.json.JSONObject;
import org.xml.sax.ContentHandler;
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

    public HashMap<String, NameObject> posiblesNombres = new HashMap<>();
    public HashMap<String, NameObject> posiblesApellidos = new HashMap<>();

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
        posiblesNombres.clear();
        posiblesApellidos.clear();
        telefonoSaved = null;
        emailSaved = null;
    }

    private void procesar(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String resultFromFile = getTextFromFile(request, response);
        if (TextUtils.isBlank(resultFromFile)) {
            throw new ServletException("No se ha podido analizar el texto del archivo");
        }
        dataFinder = DataFinder.getInstance();

        if (emailSaved == null) {
            buscarEmail(resultFromFile);
        }
        HashSet<String> palabrasResultado = new HashSet<>();
        List<String> lineas = getLineasFromResult(resultFromFile);
        lineas = limpiarLineas(lineas);
        lineas = limpiarLineasHtml(lineas);
        String cleanTextToReturn = obtainCleanText(lineas);
        lineas = ordenarLineasPorTags(lineas);
        analizarLineas(palabrasResultado, lineas);

        JSONObject jsonObject = new JSONObject();
        String apellidoSaved = buscarMaximaFrecuencia(posiblesApellidos);
        if (!TextUtils.isBlank(apellidoSaved)) {
            jsonObject.put("apellido", apellidoSaved);
        }
        String nombreSaved = buscarMaximaFrecuencia(posiblesNombres);
        if (!TextUtils.isBlank(nombreSaved)) {
            jsonObject.put("nombre", nombreSaved);
        }
        if (!TextUtils.isBlank(telefonoSaved)) {
            jsonObject.put("telefono", telefonoSaved);
        }
        if (!TextUtils.isBlank(emailSaved)) {
            jsonObject.put("email", emailSaved);
        }
        if(!TextUtils.isBlank(cleanTextToReturn)){
            jsonObject.put("texto",cleanTextToReturn);
        }
        response.getWriter().println(jsonObject);
    }

    private String obtainCleanText(List<String> lineas) {
        return String.join(" ", lineas).trim();
    }

    private List<String> limpiarLineasHtml(List<String> lineas) {
        return lineas.stream().map(s -> s.replaceAll("<meta name.*/>", "").replaceAll("<[/]?body>", "").replaceAll("<[/]?b>", "").replaceAll("<img src=.*>", "").replaceAll("<[/]?.*>","").replaceAll("[\n\r\t]", " ").replaceAll("^>","")).filter(t -> !TextUtils.isBlank(t)).collect(Collectors.toList());
    }

    private List<String> limpiarLineas(List<String> lineas) {
        return lineas.stream().filter(s -> !s.contains("<p/>") || !s.contains("<p>") || !TextUtils.isBlank(s)).collect(Collectors.toList());
    }

    private String buscarMaximaFrecuencia(HashMap<String, NameObject> listaNameObjects) {
        long avgFreq = 0;
        for (NameObject nameObject : listaNameObjects.values()) {
            avgFreq += nameObject.getFrequency();
        }
        if (!listaNameObjects.isEmpty()) {
            avgFreq /= listaNameObjects.size();
        }
        for (String key : listaNameObjects.keySet()) {
            NameObject nameObject = listaNameObjects.get(key);
            if (nameObject.isCreadoDefault()) {
                nameObject.setFrequency(avgFreq);
                listaNameObjects.put(key, nameObject);
            }
        }
        return listaNameObjects.values().stream().max(Comparator.comparingLong(NameObject::getTotalFrequency)).orElse(new NameObject("", 0)).getData();
    }

    private List<String> ordenarLineasPorTags(List<String> lineas) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (contieneTagsDatosPersonales(linea)) {
                indexes.add(i);
            }
        }
        LinkedHashSet<String> lineasOrdenadas = new LinkedHashSet<>();
        for (Integer index : indexes) {
            String linea = lineas.get(index);
            lineasOrdenadas.add(linea);
        }
        lineasOrdenadas.addAll(lineas);
        return new ArrayList(lineasOrdenadas);
    }

    private boolean contieneTagsDatosPersonales(String linea) {
        String lineaLower = linea.toLowerCase();
        return lineaLower.contains("nombre") || lineaLower.contains("apellido") || lineaLower.contains("personales");
    }


    private void analizarLineas(HashSet<String> palabrasResultado, List<String> lineas) {
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            HashSet<NameObject> nameObjectHashSet = new HashSet<>();
            String[] palabras = linea.split("[^\\wÀ-úÀ-ÿ@.#+∙-]");
            for (String palabra : palabras) {
                if (isTelefono(linea) || telefonoSaved == null && !palabra.replaceAll("\\D", "").isEmpty() && !ignorarTags(linea) && !esRangoDeAño(palabra)) {
                    String palabraTelefono = Arrays.stream(palabras).reduce("", (s, s2) -> s + s2);
                    buscarTelefono(palabraTelefono);
                }
                if (!TextUtils.isBlank(palabra) && palabra.length() > 3 && !ignorarTags(linea) && linea.replaceAll("[^0-9]", "").length() < 3) {
                    palabra = removeTextSymbol(palabra);
                    if (!palabrasResultado.contains(palabra.toUpperCase()) && !contieneSimbolosNombre(palabra)) {
                        palabrasResultado.add(palabra.toUpperCase());
                        if (i < lineas.size() - 1) {
                            String[] tempPalabras = Arrays.stream((linea + " " + lineas.get(i + 1)).split("[^\\wÀ-úÀ-ÿ@.#+∙-]")).filter(s -> !TextUtils.isBlank(s.replaceAll("^\\w", ""))).toArray(String[]::new);
                            if (tempPalabras.length <= 4) {
                                palabras = tempPalabras;
                            }
                        }
                        buscarNombres(palabras, palabra, nameObjectHashSet);
                    }

                }
            }
        }
    }

    private boolean isTelefono(String linea) {
        return StringUtils.containsAny(linea.toLowerCase(), "telefono", "tel:", "cel:", "celular");
    }

    private boolean esRangoDeAño(String palabra) {
        String textoSoloNumeros = palabra.replaceAll("\\D", "");
        if (textoSoloNumeros.length() < 8) return false;
        if (textoSoloNumeros.length() == 8) {
            String primerRango = textoSoloNumeros.substring(0, 4);
            String segundoRango = textoSoloNumeros.substring(4);
            return StringUtils.startsWithAny(primerRango, "19", "20") &&
                    StringUtils.startsWithAny(segundoRango, "19", "20");
        }
        return false;
    }

    private boolean contieneSimbolosNombre(String palabra) {
        return !TextUtils.isBlank(palabra.replaceAll("[a-zA-ZÀ-ÿ, ]", ""));
    }

    String[] TAGS_IGNORAR = {"de vida", "hoja de vida", "de nacimiento", "fecha de nacimiento",
            " de identidad", "documento de", "ciudad", "direccion", "estado civil", "año", "mes", "argentina", "colombia", "mexico", "salud", "instituto", "escuela", "tecnica", "facultad", "edificio", "universidad", "ingles", "español", "espanol", "spanish", "english", "idioma", "lenguaje", "language", "presente", "|", "dni"};

    private boolean ignorarTags(String linea) {
        for (String tag : TAGS_IGNORAR) {
            if (quitarLetrasEspeciales(linea.toLowerCase()).contains(tag)){
                return true;
            }
        }
        return false;
    }

    private void buscarNombres(String[] palabras, String palabra, HashSet<NameObject> nameObjectHashSet) {
        palabra = quitarLetrasEspeciales(palabra);
        if (buscarNombre(palabra, nameObjectHashSet) && !listaContiene(posiblesApellidos, palabra)) {
            palabras = filtrarTagsNombres(palabras);
            if (StringUtils.isAllUpperCase(palabra)) {
                palabras = Arrays.stream(palabras).filter(s -> StringUtils.isAllUpperCase(s)).toArray(String[]::new);
            }
            if (palabras.length > 5) {
                palabras = Arrays.stream(palabras).limit(5).toArray(String[]::new);
            }
            for (String palabraIter : palabras) {
                palabraIter = quitarLetrasEspeciales(palabraIter);
                if (!palabraIter.equals(palabra)) {
                    boolean esNombre = buscarNombre(palabraIter, nameObjectHashSet) && !listaContiene(posiblesApellidos, palabraIter);
                    if (buscarApellido(palabraIter, nameObjectHashSet, esNombre)) {
                        if (palabraIter.length() > 3 && !esNombre) {
                            addApellido(palabraIter, nameObjectHashSet);
                        }
                    }
                }
            }
        }
    }

    private String[] filtrarTagsNombres(String[] palabras) {
        return Arrays.stream(palabras).filter(s -> !StringUtils.equalsAny(s.toLowerCase(), "nombre", "class", "apellido", "soy", "yo", "apellidos", "nombres") && s.replaceAll("\\D", "").isEmpty() && s.length() >= 3).toArray(String[]::new);
    }

    private String quitarLetrasEspeciales(String palabra) {
        return StringUtils.stripAccents(palabra);
    }

    private void addApellido(String palabra, HashSet<NameObject> nameObjectHashSet) {
        long defaultFrequency = nameObjectHashSet.stream().min(Comparator.comparingLong(NameObject::getFrequency)).orElse(new NameObject("", 1000)).getFrequency();
        NameObject nameObject = new NameObject(TextUtilCustom.formatToName(palabra), defaultFrequency, true);
        if (contenidoEnElMail(palabra)) {
            nameObject.setFrequency(nameObject.getFrequency() * 1000);
        }
        posiblesApellidos.put(palabra.toLowerCase(), nameObject);
        if (!nameObjectHashSet.isEmpty()) {
            nameObjectHashSet.add(nameObject);
            multiplicarFrecuencias(posiblesApellidos, nameObjectHashSet);
        } else {
            nameObjectHashSet.add(nameObject);
        }
    }

    private boolean listaContiene(HashMap<String, NameObject> lista, String palabra) {
        return lista.containsKey(palabra.toLowerCase());
    }

    private String removeTextSymbol(String palabra) {
        return palabra.replaceAll("[\\r\\n\\t]", "");
    }


    private void buscarTelefono(String palabra) {
        String phoneNumber = PhoneFinder.find(palabra);
        if (!TextUtils.isBlank(phoneNumber)) {
            telefonoSaved = phoneNumber;
        }
    }

    private boolean buscarEmail(String palabra) {
        String email = EmailFinder.find(palabra);
        if (!TextUtils.isBlank(email)) {
            emailSaved = email;
            return true;
        }
        return false;
    }


    private boolean buscarNombre(String palabra, HashSet<NameObject> nameObjectHashSet) {
        NameObject nameObject = dataFinder.existeNombre(palabra);
        if (nameObject != null) {
            if (contenidoEnElMail(palabra)) {
                nameObject.setFrequency(nameObject.getFrequency() * 1000);
            }
            posiblesNombres.put(palabra.toLowerCase(), nameObject);
            if (!nameObjectHashSet.isEmpty()) {
                nameObjectHashSet.add(nameObject);
                multiplicarFrecuencias(posiblesNombres, nameObjectHashSet);
            } else {
                nameObjectHashSet.add(nameObject);
            }
        }
        return nameObject != null;
    }

    private boolean contenidoEnElMail(String palabra) {
        if (emailSaved != null && palabra.length() >= 3) {
            return emailSaved.toLowerCase().contains(palabra.toLowerCase());
        }
        return false;
    }

    private void multiplicarFrecuencias(HashMap<String, NameObject> listaNames, HashSet<NameObject> nameObjectHashSet) {
        for (NameObject nameObject : listaNames.values()) {
            if (nameObjectHashSet.contains(nameObject)) {
                long total = 0;
                for (NameObject nameIterated : nameObjectHashSet) {
                    total += nameIterated.getFrequency();
                }
                nameObject.setTotal(total * nameObjectHashSet.size());
            }
        }
    }

    private boolean buscarApellido(String palabra, HashSet<NameObject> nameObjectHashSet, boolean esNombre) {
        NameObject nameObject = dataFinder.existeApellido(palabra);
        if (nameObject != null) {
            if (!esNombre) {
                nameObject.setFrequency(nameObject.getFrequency() * 1000);
            } else {
                nameObject.setFrequency(nameObject.getFrequency() / 1000);
            }
            if (contenidoEnElMail(palabra)) {
                nameObject.setFrequency(nameObject.getFrequency() * 1000);
            }
            posiblesApellidos.put(palabra.toLowerCase(), nameObject);
            if (!nameObjectHashSet.isEmpty()) {
                nameObjectHashSet.add(nameObject);
                multiplicarFrecuencias(posiblesApellidos, nameObjectHashSet);
            } else {
                nameObjectHashSet.add(nameObject);
            }
        }
        return nameObject == null;
    }

    private List<String> getLineasFromResult(String resultFromFile) {
        String[] split = resultFromFile.split("(<[/]?p[ /]?>)|([\n\r])");
        return Arrays.stream(split).map(s -> s.replaceAll("<?[/]?p[ /]?>", "").replaceAll("\n", "")).collect(Collectors.toList());
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
                ContentHandler handler = new ToXMLContentHandler();

                AutoDetectParser parser = new AutoDetectParser();
                Metadata metadata = new Metadata();
                try (InputStream stream = item.getInputStream()) {
                    parser.parse(stream, handler, metadata);
                    result = handler.toString();
                    int firstIndex = result.indexOf("class=\"page\"") + 12;
                    int secondIndex = result.indexOf("</div>", firstIndex);
                    result = result.substring(firstIndex, secondIndex);
                }finally {
                    item.delete();
                }
            }
        } catch (Exception ex) {
        }
        return result;
    }

}

