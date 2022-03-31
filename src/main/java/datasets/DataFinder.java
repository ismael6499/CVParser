package datasets;

import com.opencsv.CSVReader;
import utils.TextUtilCustom;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class DataFinder {

    private static DataFinder instance;

    HashMap<String, NameObject> apellidos = new HashMap<>();
    HashMap<String, NameObject> nombresMasculinos = new HashMap<>();
    HashMap<String, NameObject> nombresFemeninos = new HashMap<>();

    boolean loaded = false;

    private DataFinder() {
    }

    private void cargar() {
        try {
            cargarApellidos();
            cargarNombresMasculinos();
            cargarNombresFemeninos();
            loaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarNombresFemeninos() throws Exception {
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(DataFinder.class.getResourceAsStream("/female_names.csv")))) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                String data = values[0];
                String value = values[1];
                nombresFemeninos.put(data, new NameObject(data, value));
            }
        }
    }

    private void cargarNombresMasculinos() throws Exception {
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(DataFinder.class.getResourceAsStream("/male_names.csv")))) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                String data = values[0];
                String value = values[1];
                nombresMasculinos.put(data, new NameObject(data, value));
            }
        }
    }

    private void cargarApellidos() throws Exception {
        InputStream resourceAsStream = DataFinder.class.getResourceAsStream("/surnames_freq_ge_100.csv");
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(resourceAsStream))) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                String data = values[0];
                String value = values[1];
                apellidos.put(data, new NameObject(data, value));
            }
        }
    }

    public static DataFinder getInstance() {
        if (instance == null) {
            instance = new DataFinder();
        }
        if (!instance.loaded) {
            instance.cargar();
        }
        return instance;
    }


    public NameObject existeApellido(String palabra) {
        if (palabra.length() < 3) return null;
        palabra = palabra.replaceAll("[^a-zA-ZÀ-ÿ ]", "");
        if (!apellidos.isEmpty() && apellidos.containsKey(palabra.toUpperCase())) {
            NameObject nameObject = apellidos.get(palabra.toUpperCase());
            return new NameObject(TextUtilCustom.formatToName(palabra), nameObject.getFrequency());
        }
        return null;
    }

    public NameObject existeNombre(String palabra) {
        if (palabra.length() < 3) return null;
        palabra = palabra.replaceAll("[^a-zA-ZÀ-ÿ ]", "");
        NameObject nameObject = null;
        String palabraUpper = palabra.toUpperCase();
        if (!nombresFemeninos.isEmpty()) {
            if (nombresFemeninos.containsKey(palabraUpper)) {
                nameObject = nombresFemeninos.get(palabraUpper);
            }
        }
        if (!nombresMasculinos.isEmpty() && nameObject == null)
            if (nombresMasculinos.containsKey(palabraUpper)) nameObject = nombresMasculinos.get(palabraUpper);
        if (nameObject != null) {
            return new NameObject(TextUtilCustom.formatToName(palabra), nameObject.getFrequency());
        }
        return null;
    }
}
