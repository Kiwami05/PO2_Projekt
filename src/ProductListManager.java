import java.io.*;
import java.net.Socket;
import java.util.*;

class Product implements Serializable {
    String name;
    double quantity;
    String unit;

    public Product(String name, double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    @Override
    public String toString() {
        return name + " (" + quantity + " " + unit + ")";
    }
}

class ProductListManager {
    private Map<String, Set<Product>> productList;
    final Set<String> availableUnits;
    private final String serverAddress;
    private final int serverPort;

    public ProductListManager(String serverAddress, int serverPort) {
        this.productList = new HashMap<>();
        this.availableUnits = new HashSet<>(Arrays.asList("sztuki", "kg", "m", "l"));
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void addCategory(String category) {
        sendRequest("ADD_CATEGORY", category);
    }


    // Pozostałe metody z oryginalnego kodu...
    public Map<String, Set<Product>> getProductList() {
        return productList;
    }

    public void removeProduct(String category, String name) {
        sendRequest("REMOVE_PRODUCT", category, name);
    }

    public void addProduct(String category, String name, double quantity, String unit) {
        sendRequest("ADD_PRODUCT", category, name, String.valueOf(quantity), unit);
    }

    public void editProduct(String category, String oldName, String newName, double newQuantity, String newUnit) {
        sendRequest("EDIT_PRODUCT", category, oldName, newName, String.valueOf(newQuantity), newUnit);
    }

    void sendRequest(String requestType, String... args) {
        try {
            Socket socket = new Socket(serverAddress, serverPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(requestType);
            out.writeObject(args);
            out.flush();

            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            productList = (Map<String, Set<Product>>) in.readObject();

            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void saveToFile(Map<String, Set<Product>> productList, String filePath) throws IOException {
        // Przykładowa implementacja zapisu do pliku CSV
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            for (Map.Entry<String, Set<Product>> entry : productList.entrySet()) {
                String category = entry.getKey();
                for (Product product : entry.getValue()) {
                    writer.println(category + "," + product.name + "," + product.quantity + "," + product.unit);
                }
            }
        }
    }

    public static Map<String, Set<Product>> loadFromFile(String filePath) throws IOException {
        Map<String, Set<Product>> productList = new HashMap<>();
        // Przykładowa implementacja odczytu z pliku CSV
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String category = parts[0];
                    String name = parts[1];
                    double quantity = Double.parseDouble(parts[2]);
                    String unit = parts[3];
                    Product product = new Product(name, quantity, unit);
                    productList.computeIfAbsent(category, k -> new HashSet<>()).add(product);
                }
            }
        }
        return productList;
    }

}
