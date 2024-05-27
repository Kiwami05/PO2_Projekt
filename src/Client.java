import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Client extends JFrame {
    private final ProductListManager manager;
    private final JComboBox<String> categoryComboBox;
    private final DefaultListModel<Product> productListModel;
    private final JList<Product> productList;
    private final JTextField productNameField;
    private final JTextField productQuantityField;
    private final JTextField categoryField;
    private final JComboBox<String> unitComboBox;


    public Client(String serverAddress, int serverPort) {
        super("Lista zakupów");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        manager = new ProductListManager(serverAddress, serverPort);

        // Inicjalizacja komponentów GUI
        categoryComboBox = new JComboBox<>();
        categoryComboBox.setPreferredSize(new Dimension(100, 25)); // Szerokość i wysokość
        productListModel = new DefaultListModel<>();
        productList = new JList<>(productListModel);
        productNameField = new JTextField();
        productQuantityField = new JTextField();
        unitComboBox = new JComboBox<>();


        // Ustawienie układu
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel categoryPanel = new JPanel(new FlowLayout());
        JPanel productPanel = new JPanel(new GridLayout(3, 2));
        JPanel buttonsPanel = new JPanel(new FlowLayout());


        JMenuBar menuBar = getjMenuBar();
        setJMenuBar(menuBar);

        categoryPanel.add(new JLabel("Kategoria:"));
        categoryPanel.add(categoryComboBox);
        mainPanel.add(categoryPanel, BorderLayout.NORTH);


        categoryField = new JTextField();
        categoryField.setPreferredSize(new Dimension(100, 25)); // Szerokość i wysokość
        JButton addCategoryButton = new JButton("Dodaj kategorię");
        categoryPanel.add(categoryField);
        categoryPanel.add(addCategoryButton);
        JButton removeCategoryButton = new JButton("Usuń kategorię");
        categoryPanel.add(removeCategoryButton);


        mainPanel.add(new JScrollPane(productList), BorderLayout.CENTER);

        productPanel.add(new JLabel("Nazwa produktu:"));
        productPanel.add(productNameField);
        productPanel.add(new JLabel("Ilość:"));
        productPanel.add(productQuantityField);
        productPanel.add(new JLabel("Jednostka:"));
//        productPanel.add(productUnitField);
        productPanel.add(unitComboBox);
        mainPanel.add(productPanel, BorderLayout.SOUTH);

        for (String unit : manager.availableUnits) {
            unitComboBox.addItem(unit);
        }


        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        JButton addButton = new JButton("Dodaj");
        JButton editButton = new JButton("Edytuj");
        JButton removeButton = new JButton("Usuń");
        buttonsPanel.add(addButton);
        buttonsPanel.add(editButton);
        buttonsPanel.add(removeButton);
        mainPanel.add(buttonsPanel, BorderLayout.EAST);


        addButton.addActionListener(e -> addProduct());
        editButton.addActionListener(e -> editProduct());
        removeButton.addActionListener(e -> removeProduct());
        categoryComboBox.addActionListener(e -> updateProductList());
        productList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editProduct();
                }
            }
        });

        addCategoryButton.addActionListener(e -> addCategory());
        removeCategoryButton.addActionListener(e -> removeCategory());

        add(mainPanel);
        manager.sendRequest("GET_LIST");
        updateCategoryComboBox();
        updateProductList();
        setVisible(true);
    }

    private JMenuBar getjMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Plik");
        JMenuItem openMenuItem = new JMenuItem("Otwórz");
        JMenuItem saveMenuItem = new JMenuItem("Zapisz");
        openMenuItem.addActionListener(e -> loadFromFile());
        saveMenuItem.addActionListener(e -> saveToFile());
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        menuBar.add(fileMenu);

        JMenu viewMenu = new JMenu("Lista");
        JMenuItem clearAllButton = new JMenuItem("Wyczyść listę");
        JMenuItem showAllMenuItem = new JMenuItem("Pokaż wszystkie produkty");
        clearAllButton.addActionListener(e -> clearAllProducts());
        showAllMenuItem.addActionListener(e -> displayAllProducts());
        viewMenu.add(showAllMenuItem);
        viewMenu.add(clearAllButton);
        menuBar.add(viewMenu);
        return menuBar;
    }

    private void saveToFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                ProductListManager.saveToFile(manager.getProductList(), file.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Błąd podczas zapisu do pliku: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                manager.getProductList().putAll(ProductListManager.loadFromFile(file.getAbsolutePath()));
                updateCategoryComboBox();
                updateProductList();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Błąd podczas odczytu z pliku: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Wczytywanie listy nie powiodło się, plik może być uszkodzony.", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void displayAllProducts() {
        DefaultListModel<String> allProductsModel = new DefaultListModel<>();
        for (Map.Entry<String, Set<Product>> entry : manager.getProductList().entrySet()) {
            String category = entry.getKey();
            allProductsModel.addElement(category);
            for (Product product : entry.getValue()) {
                allProductsModel.addElement("  - " + product.toString());
            }
        }

        JList<String> allProductsList = new JList<>(allProductsModel);
        JScrollPane scrollPane = new JScrollPane(allProductsList);

        JOptionPane.showMessageDialog(this, scrollPane, "Wszystkie produkty", JOptionPane.PLAIN_MESSAGE);
    }

    private void addCategory() {
        String newCategory = categoryField.getText().trim();
        if (!newCategory.isEmpty()) {
            manager.addCategory(newCategory);
            updateCategoryComboBox();
            categoryComboBox.setSelectedItem(newCategory); // Przełączenie na nową kategorię
            categoryField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Wprowadź nazwę kategorii", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void updateCategoryComboBox() {
        categoryComboBox.removeAllItems();
        for (String category : manager.getProductList().keySet()) {
            categoryComboBox.addItem(category);
        }
    }

    private void updateProductList() {
        String selectedCategory = (String) categoryComboBox.getSelectedItem();
        if (selectedCategory != null) {
            productListModel.clear();
            Set<Product> products = manager.getProductList().get(selectedCategory);
            if (products != null) {
                for (Product product : products) {
                    productListModel.addElement(product);
                }
            }
        }
    }

    private void addProduct() {
        String category = (String) categoryComboBox.getSelectedItem();

        if (category == null) {
            category = categoryField.getText().trim();
            if (category.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Wprowadź nazwę kategorii", "Błąd", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String name = productNameField.getText().trim();
        String quantityStr = productQuantityField.getText().trim();
        String unit = (String) unitComboBox.getSelectedItem();

        if (!name.isEmpty() && !quantityStr.isEmpty() && !Objects.requireNonNull(unit).isEmpty()) {
            try {
                double quantity = Double.parseDouble(quantityStr);
                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(this, "Ilość musi być większa od 0", "Błąd", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (unit.equals("sztuki") && quantity != (int) quantity) {
                    JOptionPane.showMessageDialog(this, "Dla jednostki 'sztuki' ilość musi być liczbą całkowitą", "Błąd", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                manager.addProduct(category, name, quantity, unit);
                updateProductList();
                clearFields();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Nieprawidłowa wartość ilości", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Wszystkie pola muszą być wypełnione", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editProduct() {
        Product selectedProduct = productList.getSelectedValue();
        if (selectedProduct != null) {
            String category = (String) categoryComboBox.getSelectedItem();
            String oldName = selectedProduct.name;
            double quantity = selectedProduct.quantity;
            String unit = selectedProduct.unit;

            String newName = JOptionPane.showInputDialog(this, "Wprowadź nową nazwę produktu:", oldName);
            if (newName != null) {
                String newQuantityStr = JOptionPane.showInputDialog(this, "Wprowadź nową ilość:", quantity);
                if (newQuantityStr != null) {
                    try {
                        double newQuantity = Double.parseDouble(newQuantityStr);
                        JComboBox<String> unitComboBox = new JComboBox<>(new Vector<>(manager.availableUnits));
                        unitComboBox.setSelectedItem(unit); // Ustawienie domyślnej jednostki

                        int result = JOptionPane.showConfirmDialog(this, unitComboBox, "Wybierz nową jednostkę miary", JOptionPane.OK_CANCEL_OPTION);
                        if (result == JOptionPane.OK_OPTION) {
                            String newUnit = (String) unitComboBox.getSelectedItem();
                            manager.editProduct(category, oldName, newName, newQuantity, newUnit);
                            updateProductList();
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Nieprawidłowa wartość ilości", "Błąd", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Wybierz produkt do edycji", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeProduct() {
        Product selectedProduct = productList.getSelectedValue();
        if (selectedProduct != null) {
            String category = (String) categoryComboBox.getSelectedItem();
            int confirm = JOptionPane.showConfirmDialog(this, "Czy na pewno chcesz usunąć produkt " + selectedProduct.name + "?", "Potwierdzenie", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                manager.removeProduct(category, selectedProduct.name);
                updateProductList();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Wybierz produkt do usunięcia", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAllProducts() {
        int confirm = JOptionPane.showConfirmDialog(this, "Czy na pewno chcesz usunąć wszystkie produkty?", "Potwierdzenie", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            manager.getProductList().clear();
            updateProductList();
            updateCategoryComboBox();
        }
    }

    private void removeCategory() {
        String selectedCategory = (String) categoryComboBox.getSelectedItem();
        if (selectedCategory != null) {
            int confirm = JOptionPane.showConfirmDialog(this, "Czy na pewno chcesz usunąć kategorię \"" + selectedCategory + "\"?", "Potwierdzenie", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                manager.getProductList().remove(selectedCategory);
                updateProductList();
                updateCategoryComboBox();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Wybierz kategorię do usunięcia", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        productNameField.setText("");
        productQuantityField.setText("");
    }

    public static void main(String[] args) {
        String serverAddress = JOptionPane.showInputDialog(null, "Wprowadź adres IP serwera:", "127.0.0.1");
        int serverPort = 8000; // Ustaw port serwera

        SwingUtilities.invokeLater(() -> new Client(serverAddress, serverPort).setVisible(true));
    }
}
