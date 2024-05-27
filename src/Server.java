import java.io.*;
import java.net.Socket;
import java.util.*;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Server {
    private static final int SERVER_PORT = 8000;
    private static final int THREAD_POOL_SIZE = 5;
    private static final Map<String, Set<Product>> sharedProductList = new HashMap<>();
    private static final Set<String> availableUnits = new HashSet<>(Arrays.asList("sztuki", "kg", "m", "l"));

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Serwer uruchomiony na porcie " + SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.execute(() -> handleClientRequest(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClientRequest(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {


            String requestType = (String) in.readObject();
            String[] args = (String[]) in.readObject();

            synchronized (sharedProductList) {
                switch (requestType) {
                    case "ADD_CATEGORY":
                        addCategory(args[0]);
                        break;
                    case "REMOVE_PRODUCT":
                        removeProduct(args[0], args[1]);
                        break;
                    case "ADD_PRODUCT":
                        addProduct(args[0], args[1], Double.parseDouble(args[2]), args[3]);
                        break;
                    case "EDIT_PRODUCT":
                        editProduct(args[0], args[1], args[2], Double.parseDouble(args[3]), args[4]);
                        break;
                }
            }

            out.writeObject(sharedProductList);
            out.flush();

            clientSocket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void addCategory(String category) {
        sharedProductList.putIfAbsent(category, new HashSet<>());
    }

    private static void removeProduct(String category, String name) {
        Set<Product> products = sharedProductList.get(category);
        if (products != null) {
            products.removeIf(product -> product.name.equals(name));
        }
    }

    private static void addProduct(String category, String name, double quantity, String unit) {
        if (!availableUnits.contains(unit)) {
            System.err.println("Nieprawidłowa jednostka miary: " + unit);
            return;
        }

        Product product = new Product(name, quantity, unit);
        sharedProductList.computeIfAbsent(category, k -> new HashSet<>()).add(product);
    }

    private static void editProduct(String category, String oldName, String newName, double newQuantity, String newUnit) {
        if (!availableUnits.contains(newUnit)) {
            System.err.println("Nieprawidłowa jednostka miary: " + newUnit);
            return;
        }

        Set<Product> products = sharedProductList.get(category);
        if (products != null) {
            for (Product product : products) {
                if (product.name.equals(oldName)) {
                    products.remove(product);
                    Product newProduct = new Product(newName, newQuantity, newUnit);
                    products.add(newProduct);
                    return;
                }
            }
        }
        System.err.println("Nie znaleziono produktu: " + oldName + " w kategorii " + category);
    }
}
