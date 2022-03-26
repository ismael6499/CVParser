package datasets;

import com.opencsv.CSVReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

public class DataFinder {

    private static DataFinder instance;

    HashSet<String> apellidos = new HashSet<>();
    HashSet<String> nombresMasculinos = new HashSet<>();
    HashSet<String> nombresFemeninos = new HashSet<>();

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
                nombresFemeninos.add(values[0]);
            }
        }
    }

    private void cargarNombresMasculinos() throws Exception {
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(DataFinder.class.getResourceAsStream("/male_names.csv")))) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                nombresMasculinos.add(values[0]);
            }
        }
    }

    private void cargarApellidos() throws Exception {
        InputStream resourceAsStream = DataFinder.class.getResourceAsStream("/surnames_freq_ge_100.csv");
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(resourceAsStream))) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                apellidos.add(values[0]);
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


    public boolean existeApellido(String palabra) {
        return !apellidos.isEmpty() && apellidos.contains(palabra);
    }

    public boolean existeNombre(String palabra) {
        if (!nombresFemeninos.isEmpty()) if(nombresFemeninos.contains(palabra)) return true;
        if (!nombresMasculinos.isEmpty()) return nombresMasculinos.contains(palabra);
        return false;

    }
}
