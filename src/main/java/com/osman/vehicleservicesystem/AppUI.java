package com.osman.vehicleservicesystem;

import com.formdev.flatlaf.FlatLightLaf;
import com.osman.vehicleservicesystem.dao.*;
import com.osman.vehicleservicesystem.model.*;
import com.osman.vehicleservice.db.DBConnection;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import java.awt.*;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AppUI extends JFrame {

    // DAOs
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final DriverDAO driverDAO = new DriverDAO();
    private final MaintenanceDAO maintenanceDAO = new MaintenanceDAO();
    private final AssignmentDAO assignmentDAO = new AssignmentDAO();
    private final ReportDAO reportDAO = new ReportDAO();

    // Active maps (assignment -> vehicle/driver)
    private java.util.Map<Long, String> activeDriverNameByVehicle = new java.util.HashMap<>();
    private java.util.Map<Long, String> activeVehiclePlateByDriver = new java.util.HashMap<>();

    // ===== Vehicles UI =====
    private DefaultTableModel vehicleModel;
    private JTable vehicleTable;
    private JTextField vehicleSearch;
    private JTextField vId, vPlate, vVin, vMake, vModel, vYear, vKm;
    private JComboBox<String> vStatus;

    // ===== Drivers UI =====
    private DefaultTableModel driverModel;
    private JTable driverTable;
    private JTextField driverSearch;
    private JTextField dId, dFn, dLn, dLic, dPhone;

    // ===== Maintenance UI =====
    private JComboBox<ComboItem> maintVehicleCombo;
    private DefaultTableModel maintModel;
    private JTable maintTable;
    private JTextField mId, mDate, mType, mKm, mCost, mDesc;

    // ===== Assignments UI =====
    private DefaultTableModel asgModel;
    private JTable asgTable;
    private JTextField cDriver, cVehicle, cStartKm, cStartDt;
    private JTextField clAsgId, clEndKm, clEndDt;

    // ===== Reports UI =====
    private JTextArea reportArea;

    public AppUI() {
        super("VehicleServiceSystem - KorkmazBMW");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1050, 650);
        setLocationRelativeTo(null);

        applyUiDefaults();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Vehicles", buildVehiclesPanel());
        tabs.addTab("Drivers", buildDriversPanel());
        tabs.addTab("Maintenance", buildMaintenancePanel());
        tabs.addTab("Assignments", buildAssignmentsPanel());
        tabs.addTab("Reports", buildReportsPanel());

        setContentPane(tabs);

        // initial loads
        refreshActiveAssignmentMaps();
        refreshVehicles();
        refreshDrivers();
        refreshMaintenanceVehicleCombo();
        refreshActiveAssignments();
    }

    // =======================================================
    // Change Active Vehicle Dialog
    // =======================================================
    private void openChangeActiveVehicleDialogForSelectedDriver() {
        safe(() -> {
            int viewRow = driverTable.getSelectedRow();
            if (viewRow < 0) {
                info("Önce bir driver seç.");
                return;
            }

            int modelRow = driverTable.convertRowIndexToModel(viewRow);
            long driverId = Long.parseLong(val(driverModel, modelRow, 0));
            String driverLabel = val(driverModel, modelRow, 1) + " " + val(driverModel, modelRow, 2);

            refreshActiveAssignmentMaps();

            // Aktif assignment var mı kontrol et
            Assignment active = getActiveAssignmentForDriver(driverId);
            List<Vehicle> allVehicles = vehicleDAO.findAll();

            // Araç seçim kutusu
            DefaultComboBoxModel<ComboItem> vehicleChoices = new DefaultComboBoxModel<>();
            for (Vehicle v : allVehicles) {
                boolean busy = activeDriverNameByVehicle.containsKey(v.getVehicleId());
                if (!busy) {
                    vehicleChoices.addElement(new ComboItem(
                            v.getVehicleId(),
                            v.getPlateNo() + " | KM=" + v.getCurrentKm()
                    ));
                }
            }

            // Eğer aktif assignment yoksa => yeni atama
            if (active == null) {
                if (vehicleChoices.getSize() == 0) {
                    info("Boşta araç yok.");
                    return;
                }

                JComboBox<ComboItem> cbVehicle = new JComboBox<>(vehicleChoices);
                JTextField tfStartKm = new JTextField("0");
                JTextField tfStartDt = new JTextField(nowTsString());

                JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
                panel.add(new JLabel("Driver:")); panel.add(new JLabel(driverLabel));
                panel.add(new JLabel("Vehicle:")); panel.add(cbVehicle);
                panel.add(new JLabel("Start KM:")); panel.add(tfStartKm);
                panel.add(new JLabel("Start DT:")); panel.add(tfStartDt);

                int res = JOptionPane.showConfirmDialog(this, panel, "Assign Vehicle",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (res != JOptionPane.OK_OPTION) return;

                ComboItem selected = (ComboItem) cbVehicle.getSelectedItem();
                if (selected == null) { info("Araç seç."); return; }

                long skm = Long.parseLong(tfStartKm.getText().trim());
                Timestamp sdt = Timestamp.valueOf(tfStartDt.getText().trim());

                assignmentDAO.createAssignmentSP(driverId, selected.id, skm, sdt);
            } else {
                // Aktif varsa => değiştir
                String currentPlate = activeVehiclePlateByDriver.getOrDefault(driverId, "(bilinmiyor)");
                JComboBox<ComboItem> cbNewVehicle = new JComboBox<>(vehicleChoices);
                JTextField tfEndKm = new JTextField(String.valueOf(active.getStartKm() + 10));
                JTextField tfEndDt = new JTextField(nowTsString());

                JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
                panel.add(new JLabel("Driver:")); panel.add(new JLabel(driverLabel));
                panel.add(new JLabel("Current Vehicle:")); panel.add(new JLabel(currentPlate));
                panel.add(new JLabel("End KM:")); panel.add(tfEndKm);
                panel.add(new JLabel("End DT:")); panel.add(tfEndDt);
                panel.add(new JLabel("New Vehicle:")); panel.add(cbNewVehicle);

                int res = JOptionPane.showConfirmDialog(this, panel, "Change Active Vehicle",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (res != JOptionPane.OK_OPTION) return;

                long endKm = Long.parseLong(tfEndKm.getText().trim());
                Timestamp endTs = Timestamp.valueOf(tfEndDt.getText().trim());
                ComboItem newVehicle = (ComboItem) cbNewVehicle.getSelectedItem();
                if (newVehicle == null) { info("Yeni araç seç."); return; }

                assignmentDAO.closeAssignmentSP(active.getAssignmentId(), endKm, endTs);
                assignmentDAO.createAssignmentSP(driverId, newVehicle.id, endKm, Timestamp.valueOf(nowTsString()));
            }

            refreshActiveAssignmentMaps();
            refreshDrivers();
            refreshVehicles();
            refreshActiveAssignments();
            info("İşlem tamamlandı.");
        });
    }

    private void openAssignVehicleDialogForSelectedDriver() {
        safe(() -> {
            int viewRow = driverTable.getSelectedRow();
            if (viewRow < 0) { info("Önce bir driver seç."); return; }

            int modelRow = driverTable.convertRowIndexToModel(viewRow);

            long driverId = Long.parseLong(val(driverModel, modelRow, 0));
            String driverLabel = val(driverModel, modelRow, 1) + " " + val(driverModel, modelRow, 2);

            // Driver zaten aktif bir araca bağlı mı?
            refreshActiveAssignmentMaps();
            if (activeVehiclePlateByDriver.containsKey(driverId)) {
                info("Bu driver zaten aktif bir araca bağlı: " + activeVehiclePlateByDriver.get(driverId)
                        + "\nÖnce Assignments tabından assignment'ı kapat.");
                return;
            }

            // Boşta araç listesi
            DefaultComboBoxModel<ComboItem> vehicleChoices = new DefaultComboBoxModel<>();
            List<Vehicle> allVehicles = vehicleDAO.findAll();
            for (Vehicle v : allVehicles) {
                boolean vehicleBusy = activeDriverNameByVehicle.containsKey(v.getVehicleId()); // aktif assignment varsa
                if (!vehicleBusy) {
                    String label = v.getPlateNo() + " | KM=" + v.getCurrentKm();
                    vehicleChoices.addElement(new ComboItem(v.getVehicleId(), label));
                }
            }

            if (vehicleChoices.getSize() == 0) {
                info("Boşta araç yok. Önce bir assignment kapatman lazım.");
                return;
            }

            JComboBox<ComboItem> cbVehicle = new JComboBox<>(vehicleChoices);

            JTextField tfStartKm = new JTextField("0");
            JTextField tfStartDt = new JTextField(nowTsString());

            // seçili araca göre KM otomatik dolsun
            ComboItem first = (ComboItem) cbVehicle.getSelectedItem();
            if (first != null) {
                long vid = first.id;
                for (Vehicle v : allVehicles) {
                    if (v.getVehicleId() == vid) {
                        tfStartKm.setText(String.valueOf(v.getCurrentKm()));
                        break;
                    }
                }
            }

            cbVehicle.addActionListener(ev -> {
                ComboItem it = (ComboItem) cbVehicle.getSelectedItem();
                if (it == null) return;
                long vid = it.id;
                for (Vehicle v : allVehicles) {
                    if (v.getVehicleId() == vid) {
                        tfStartKm.setText(String.valueOf(v.getCurrentKm()));
                        break;
                    }
                }
            });

            JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
            p.add(new JLabel("Driver:"));
            p.add(new JLabel(driverId + " - " + driverLabel));
            p.add(new JLabel("Vehicle (available):"));
            p.add(cbVehicle);
            p.add(new JLabel("Start KM:"));
            p.add(tfStartKm);
            p.add(new JLabel("Start DateTime (YYYY-MM-DD HH:MM:SS):"));
            p.add(tfStartDt);

            int res = JOptionPane.showConfirmDialog(
                    this, p, "Assign Vehicle", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
            );
            if (res != JOptionPane.OK_OPTION) return;

            ComboItem vi = (ComboItem) cbVehicle.getSelectedItem();
            if (vi == null) { info("Araç seç."); return; }

            long startKm = Long.parseLong(tfStartKm.getText().trim());
            Timestamp startTs = Timestamp.valueOf(tfStartDt.getText().trim());

            assignmentDAO.createAssignmentSP(driverId, vi.id, startKm, startTs);

            info("Vehicle assigned (active).");

            // refresh all
            refreshActiveAssignmentMaps();
            refreshDrivers();
            refreshVehicles();
            refreshActiveAssignments();
        });
    }

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LocalDateTime parseStartDtFromTable(int modelRow) {
        // asgModel kolon sırası: ID, DriverID, VehicleID, StartDT, StartKM
        Object o = asgModel.getValueAt(modelRow, 3);
        if (o == null) throw new IllegalArgumentException("StartDT boş.");

        String s = String.valueOf(o).trim();

        // JTable bazen "2025-12-26 03:21:06.0" gibi getiriyor -> .0 kısmını at
        if (s.contains(".")) s = s.substring(0, s.indexOf('.'));

        return LocalDateTime.parse(s.replace('T', ' '), DT_FMT);
    }

    private String formatDt(LocalDateTime dt) {
        return dt.format(DT_FMT);
    }

    // =========================================================
    // VEHICLES
    // =========================================================
    private JPanel buildVehiclesPanel() {
        JPanel root = pad(new BorderLayout(10, 10));

        vehicleSearch = new JTextField();
        JButton btnSearch = new JButton("Search (Plate/VIN)");
        JButton btnRefresh = new JButton("Refresh");
        root.add(searchBar("Search:", vehicleSearch, btnSearch, btnRefresh), BorderLayout.NORTH);

        vehicleModel = nonEditableModel("ID","Plate","VIN","Make","Model","Year","KM","Status","Active Driver");
        vehicleTable = new JTable(vehicleModel);

        // Sorter (ID numeric)
        TableRowSorter<DefaultTableModel> vehicleSorter = new TableRowSorter<>(vehicleModel);
        vehicleTable.setRowSorter(vehicleSorter);

        vehicleSorter.setComparator(0, (a, b) -> {
            try {
                long x = Long.parseLong(String.valueOf(a));
                long y = Long.parseLong(String.valueOf(b));
                return Long.compare(x, y);
            } catch (Exception e) {
                return String.valueOf(a).compareTo(String.valueOf(b));
            }
        });
        vehicleSorter.setSortKeys(java.util.Collections.singletonList(
                new RowSorter.SortKey(0, SortOrder.ASCENDING)
        ));

        root.add(new JScrollPane(vehicleTable), BorderLayout.CENTER);

        // form fields
        vId = tf(false);
        vId.setVisible(false);

        vPlate = tf(true);
        vVin = tf(true);
        vMake = tf(true);
        vModel = tf(true);
        vYear = tf(true);
        vKm = tf(true);

        // IMPORTANT: ComboBox burada initialize edilmeli (selection listener içinde değil)
        vStatus = new JComboBox<>(new String[]{"ACTIVE","IN_SERVICE","ASSIGNED","INACTIVE"});
        vStatus.setSelectedItem("ACTIVE");

        JPanel form = labeledGrid(
                new String[]{"Plate","VIN","Make","Model","Year","KM","Status"},
                new JComponent[]{vPlate, vVin, vMake, vModel, vYear, vKm, vStatus}
        );

        JButton btnClear = new JButton("Clear Form");
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update Selected");
        JButton btnDelete = new JButton("Delete Selected");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.add(btnClear);
        actions.add(btnUpdate);
        actions.add(btnAdd);
        actions.add(btnDelete);

        JPanel south = new JPanel(new BorderLayout(10, 10));
        south.add(form, BorderLayout.CENTER);
        south.add(actions, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        // events
        btnRefresh.addActionListener(e -> {
            refreshActiveAssignmentMaps();
            refreshVehicles();
        });
        btnSearch.addActionListener(e -> searchVehicles());
        btnClear.addActionListener(e -> clearVehicleForm());

        vehicleTable.getSelectionModel().addListSelectionListener(e -> {
            if (ignoreSelection(e)) return;
            int viewRow = vehicleTable.getSelectedRow();
            if (viewRow < 0) return;

            int modelRow = vehicleTable.convertRowIndexToModel(viewRow);

            vId.setText(val(vehicleModel, modelRow, 0));
            vPlate.setText(val(vehicleModel, modelRow, 1));
            vVin.setText(val(vehicleModel, modelRow, 2));
            vMake.setText(val(vehicleModel, modelRow, 3));
            vModel.setText(val(vehicleModel, modelRow, 4));
            vYear.setText(val(vehicleModel, modelRow, 5));
            vKm.setText(val(vehicleModel, modelRow, 6));

            // Status kolonu 7
            String status = val(vehicleModel, modelRow, 7);
            if (status == null || status.isBlank()) status = "ACTIVE";
            vStatus.setSelectedItem(status);
        });

        btnAdd.addActionListener(e -> safe(() -> {
            Vehicle v = new Vehicle();
            v.setPlateNo(text(vPlate));
            v.setVinNo(text(vVin));
            v.setMake(text(vMake));
            v.setModel(text(vModel));
            v.setYear(parseInt(vYear, "Year"));
            v.setCurrentKm(parseLong(vKm, "KM"));
            v.setStatus((String) vStatus.getSelectedItem());
            v.setColour(null);
            v.setNotes("Added via UI");
            v.setServiceEntryDate(Date.valueOf(LocalDate.now()));

            long id = vehicleDAO.insert(v);
            info("Vehicle inserted. ID=" + id);

            refreshMaintenanceVehicleCombo();
            refreshVehicles();
            clearVehicleForm();
        }));

        btnUpdate.addActionListener(e -> safe(() -> {
            if (text(vId).isEmpty()) { info("Select a row first."); return; }

            Vehicle v = new Vehicle();
            v.setVehicleId(parseLong(vId, "Vehicle ID"));
            v.setPlateNo(text(vPlate));
            v.setVinNo(text(vVin));
            v.setMake(text(vMake));
            v.setModel(text(vModel));
            v.setYear(parseInt(vYear, "Year"));
            v.setCurrentKm(parseLong(vKm, "KM"));
            v.setStatus((String) vStatus.getSelectedItem());
            v.setColour(null);
            v.setNotes("Updated via UI");
            v.setServiceEntryDate(Date.valueOf(LocalDate.now()));

            boolean ok = vehicleDAO.update(v);
            info(ok ? "Vehicle updated." : "Update failed.");

            refreshMaintenanceVehicleCombo();
            refreshVehicles();
        }));

        btnDelete.addActionListener(e -> safe(() -> {
            int viewRow = vehicleTable.getSelectedRow();
            if (viewRow < 0) { info("Select a row first."); return; }

            int modelRow = vehicleTable.convertRowIndexToModel(viewRow);

            long id = Long.parseLong(val(vehicleModel, modelRow, 0));
            if (!confirm("Delete vehicle ID=" + id + " ?")) return;

            boolean ok = vehicleDAO.deleteById(id);
            info(ok ? "Deleted." : "Delete failed.");

            refreshMaintenanceVehicleCombo();
            refreshVehicles();
            clearVehicleForm();
        }));

        return root;
    }

    private void refreshVehicles() {
        safe(() -> {
            vehicleModel.setRowCount(0);
            for (Vehicle v : vehicleDAO.findAll()) {
                String activeDriver = activeDriverNameByVehicle.getOrDefault(v.getVehicleId(), "-");

                vehicleModel.addRow(new Object[]{
                        v.getVehicleId(), v.getPlateNo(), v.getVinNo(),
                        v.getMake(), v.getModel(), v.getYear(),
                        v.getCurrentKm(), v.getStatus(),
                        activeDriver
                });
            }
        });
    }

    private void searchVehicles() {
        safe(() -> {
            String term = text(vehicleSearch);
            vehicleModel.setRowCount(0);

            List<Vehicle> list = term.isEmpty()
                    ? vehicleDAO.findAll()
                    : vehicleDAO.searchByPlateOrVin(term);

            for (Vehicle v : list) {
                String activeDriver = activeDriverNameByVehicle.getOrDefault(v.getVehicleId(), "-");

                vehicleModel.addRow(new Object[]{
                        v.getVehicleId(), v.getPlateNo(), v.getVinNo(),
                        v.getMake(), v.getModel(), v.getYear(),
                        v.getCurrentKm(), v.getStatus(),
                        activeDriver
                });
            }
        });
    }

    private void clearVehicleForm() {
        vehicleTable.clearSelection();
        vId.setText("");
        vPlate.setText("");
        vVin.setText("");
        vMake.setText("");
        vModel.setText("");
        vYear.setText("");
        vKm.setText("");
        vStatus.setSelectedItem("ACTIVE");
    }

    // =========================================================
    // DRIVERS
    // =========================================================
    private JPanel buildDriversPanel() {
        JPanel root = pad(new BorderLayout(10, 10));

        driverSearch = new JTextField();
        JButton btnSearch = new JButton("Search (Name/License)");
        JButton btnRefresh = new JButton("Refresh");
        root.add(searchBar("Search:", driverSearch, btnSearch, btnRefresh), BorderLayout.NORTH);

        driverModel = nonEditableModel("ID", "First Name", "Last Name", "License", "Phone", "Active Vehicle");
        driverTable = new JTable(driverModel);

        // Sorter (ID numeric)
        TableRowSorter<DefaultTableModel> driverSorter = new TableRowSorter<>(driverModel);
        driverTable.setRowSorter(driverSorter);

        driverSorter.setComparator(0, (a, b) -> {
            try {
                long x = Long.parseLong(String.valueOf(a));
                long y = Long.parseLong(String.valueOf(b));
                return Long.compare(x, y);
            } catch (Exception e) {
                return String.valueOf(a).compareTo(String.valueOf(b));
            }
        });
        driverSorter.setSortKeys(java.util.Collections.singletonList(
                new RowSorter.SortKey(0, SortOrder.ASCENDING)
        ));

        root.add(new JScrollPane(driverTable), BorderLayout.CENTER);

        dId = tf(false); dId.setVisible(false);
        dFn = tf(true);
        dLn = tf(true);
        dLic = tf(true);
        dPhone = tf(true);

        JPanel form = labeledGrid(
                new String[]{"First Name","Last Name","License No","Phone"},
                new JComponent[]{dFn, dLn, dLic, dPhone}
        );

        JButton btnClear = new JButton("Clear Form");
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update Selected");
        JButton btnDelete = new JButton("Delete Selected");
        JButton btnAssignVehicle = new JButton("Assign Vehicle");
        btnAssignVehicle.setEnabled(false);
        btnAssignVehicle.addActionListener(e -> openAssignVehicleDialogForSelectedDriver());
        JButton btnChangeActiveVehicle = new JButton("Change Active Vehicle");
        btnChangeActiveVehicle.setEnabled(false);
        btnChangeActiveVehicle.addActionListener(e -> openChangeActiveVehicleDialogForSelectedDriver());
        btnAssignVehicle.setEnabled(true);
        btnChangeActiveVehicle.setEnabled(true);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.add(btnClear);
        actions.add(btnUpdate);
        actions.add(btnAdd);
        actions.add(btnDelete);
        actions.add(btnAssignVehicle);
        actions.add(btnChangeActiveVehicle);

        JPanel south = new JPanel(new BorderLayout(10, 10));
        south.add(form, BorderLayout.CENTER);
        south.add(actions, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> {
            refreshActiveAssignmentMaps();
            refreshDrivers();
        });
        btnSearch.addActionListener(e -> searchDrivers());
        btnClear.addActionListener(e -> clearDriverForm());

        driverTable.getSelectionModel().addListSelectionListener(e -> {
            if (ignoreSelection(e)) return;
            int viewRow = driverTable.getSelectedRow();
            if (viewRow < 0) return;

            int modelRow = driverTable.convertRowIndexToModel(viewRow);

            dId.setText(val(driverModel, modelRow, 0));
            dFn.setText(val(driverModel, modelRow, 1));
            dLn.setText(val(driverModel, modelRow, 2));
            dLic.setText(val(driverModel, modelRow, 3));
            dPhone.setText(val(driverModel, modelRow, 4));

            btnAssignVehicle.setEnabled(true);
        });

        btnAdd.addActionListener(e -> safe(() -> {
            Driver d = new Driver();
            d.setFirstName(text(dFn));
            d.setLastName(text(dLn));
            d.setLicenseNo(text(dLic));
            d.setPhone(text(dPhone));

            long id = driverDAO.insert(d);
            info("Driver inserted. ID=" + id);

            refreshDrivers();
            clearDriverForm();
        }));

        btnUpdate.addActionListener(e -> safe(() -> {
            if (text(dId).isEmpty()) { info("Select a row first."); return; }

            Driver d = new Driver();
            d.setDriverId(parseLong(dId, "Driver ID"));
            d.setFirstName(text(dFn));
            d.setLastName(text(dLn));
            d.setLicenseNo(text(dLic));
            d.setPhone(text(dPhone));

            boolean ok = driverDAO.update(d);
            info(ok ? "Driver updated." : "Update failed.");

            refreshDrivers();
        }));

        btnDelete.addActionListener(e -> safe(() -> {
            int viewRow = driverTable.getSelectedRow();
            if (viewRow < 0) { info("Select a row first."); return; }

            int modelRow = driverTable.convertRowIndexToModel(viewRow);

            long id = Long.parseLong(val(driverModel, modelRow, 0));
            if (!confirm("Delete driver ID=" + id + " ?")) return;

            boolean ok = driverDAO.deleteById(id);
            info(ok ? "Deleted." : "Delete failed.");

            refreshDrivers();
            clearDriverForm();
        }));

        return root;
    }

    private void refreshDrivers() {
        safe(() -> {
            driverModel.setRowCount(0);
            for (Driver d : driverDAO.findAll()) {
                String activeVehicle = activeVehiclePlateByDriver.getOrDefault(d.getDriverId(), "-");

                driverModel.addRow(new Object[]{
                        d.getDriverId(), d.getFirstName(), d.getLastName(),
                        d.getLicenseNo(), d.getPhone(),
                        activeVehicle
                });
            }
        });
    }

    private void searchDrivers() {
        safe(() -> {
            String term = text(driverSearch);
            driverModel.setRowCount(0);

            List<Driver> list = term.isEmpty()
                    ? driverDAO.findAll()
                    : driverDAO.searchByNameOrLicense(term);

            for (Driver d : list) {
                String activeVehicle = activeVehiclePlateByDriver.getOrDefault(d.getDriverId(), "-");

                driverModel.addRow(new Object[]{
                        d.getDriverId(), d.getFirstName(), d.getLastName(),
                        d.getLicenseNo(), d.getPhone(),
                        activeVehicle
                });
            }
        });
    }

    private void clearDriverForm() {
        driverTable.clearSelection();
        dId.setText("");
        dFn.setText("");
        dLn.setText("");
        dLic.setText("");
        dPhone.setText("");
    }

    // =========================================================
    // MAINTENANCE
    // =========================================================
    private JPanel buildMaintenancePanel() {
        JPanel root = pad(new BorderLayout(10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        maintVehicleCombo = new JComboBox<>();
        JButton btnLoad = new JButton("Load History");
        JButton btnRefreshVehicles = new JButton("Refresh Vehicles");
        top.add(new JLabel("Vehicle:"));
        top.add(maintVehicleCombo);
        top.add(btnLoad);
        top.add(btnRefreshVehicles);

        maintModel = nonEditableModel("MaintID","Date","Type","KM","Cost","VehicleID","Description");
        maintTable = new JTable(maintModel);

        // form
        mId = tf(false); mId.setEditable(false);
        mDate = new JTextField("2025-12-23");
        mType = new JTextField("General Check");
        mKm = new JTextField("0");
        mCost = new JTextField("0");
        mDesc = new JTextField("Added via UI");

        JPanel form = labeledGrid(
                new String[]{"Maint ID","Date (YYYY-MM-DD)","Type","KM","Cost","Description"},
                new JComponent[]{mId, mDate, mType, mKm, mCost, mDesc}
        );

        JButton btnClear = new JButton("Clear Form");
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update Selected");
        JButton btnDelete = new JButton("Delete Selected");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.add(btnClear);
        actions.add(btnAdd);
        actions.add(btnUpdate);
        actions.add(btnDelete);

        JPanel south = new JPanel(new BorderLayout(10, 10));
        south.add(form, BorderLayout.CENTER);
        south.add(actions, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(maintTable), BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        btnRefreshVehicles.addActionListener(e -> refreshMaintenanceVehicleCombo());
        btnLoad.addActionListener(e -> loadMaintenanceForSelectedVehicle());
        btnClear.addActionListener(e -> clearMaintenanceForm());

        maintTable.getSelectionModel().addListSelectionListener(e -> {
            if (ignoreSelection(e)) return;
            int viewRow = maintTable.getSelectedRow();
            if (viewRow < 0) return;

            int modelRow = maintTable.convertRowIndexToModel(viewRow);

            mId.setText(val(maintModel, modelRow, 0));
            mDate.setText(val(maintModel, modelRow, 1));
            mType.setText(val(maintModel, modelRow, 2));
            mKm.setText(val(maintModel, modelRow, 3));
            mCost.setText(val(maintModel, modelRow, 4));
            mDesc.setText(val(maintModel, modelRow, 6));
        });

        btnAdd.addActionListener(e -> safe(() -> {
            ComboItem it = (ComboItem) maintVehicleCombo.getSelectedItem();
            if (it == null) { info("Select a vehicle."); return; }

            Maintenance m = new Maintenance();
            m.setVehicleId(Math.toIntExact(it.id));
            m.setMaintDate(Date.valueOf(text(mDate)));
            m.setMaintType(text(mType));
            m.setOdometerKm(parseInt(mKm, "KM"));
            m.setCost(parseDouble(mCost, "Cost"));
            m.setDescription(text(mDesc));

            long id = maintenanceDAO.insert(m);
            info("Maintenance inserted. ID=" + id);

            loadMaintenanceForSelectedVehicle();
            clearMaintenanceForm();
        }));

        btnUpdate.addActionListener(e -> safe(() -> {
            if (text(mId).isEmpty()) { info("Select a maintenance row first."); return; }

            ComboItem it = (ComboItem) maintVehicleCombo.getSelectedItem();
            if (it == null) { info("Select a vehicle."); return; }

            Maintenance m = new Maintenance();
            m.setMaintId(parseInt(mId, "Maint ID"));
            m.setVehicleId(Math.toIntExact(it.id));
            m.setMaintDate(Date.valueOf(text(mDate)));
            m.setMaintType(text(mType));
            m.setOdometerKm(parseInt(mKm, "KM"));
            m.setCost(parseDouble(mCost, "Cost"));
            m.setDescription(text(mDesc));

            boolean ok = maintenanceDAO.update(m);
            info(ok ? "Updated." : "Update failed.");

            loadMaintenanceForSelectedVehicle();
        }));

        btnDelete.addActionListener(e -> safe(() -> {
            if (text(mId).isEmpty()) { info("Select a maintenance row first."); return; }

            int id = parseInt(mId, "Maint ID");
            if (!confirm("Delete maintenance ID=" + id + " ?")) return;

            boolean ok = maintenanceDAO.deleteById(id);
            info(ok ? "Deleted." : "Delete failed.");

            loadMaintenanceForSelectedVehicle();
            clearMaintenanceForm();
        }));

        return root;
    }

    private void refreshMaintenanceVehicleCombo() {
        safe(() -> {
            maintVehicleCombo.removeAllItems();
            for (Vehicle v : vehicleDAO.findAll()) {
                maintVehicleCombo.addItem(new ComboItem(v.getVehicleId(), v.getPlateNo()));
            }
        });
    }

    private void loadMaintenanceForSelectedVehicle() {
        safe(() -> {
            ComboItem it = (ComboItem) maintVehicleCombo.getSelectedItem();
            if (it == null) return;

            maintModel.setRowCount(0);
            List<Maintenance> list = maintenanceDAO.findByVehicleId(Math.toIntExact(it.id));
            for (Maintenance m : list) {
                maintModel.addRow(new Object[]{
                        m.getMaintId(), m.getMaintDate(), m.getMaintType(),
                        m.getOdometerKm(), m.getCost(), m.getVehicleId(), m.getDescription()
                });
            }
        });
    }

    private void clearMaintenanceForm() {
        maintTable.clearSelection();
        mId.setText("");
        mDate.setText("2025-12-23");
        mType.setText("General Check");
        mKm.setText("0");
        mCost.setText("0");
        mDesc.setText("");
    }

    // =========================================================
    // ASSIGNMENTS (SP)
    // =========================================================
    private JPanel buildAssignmentsPanel() {
        JPanel root = pad(new BorderLayout(10, 10));

        JButton btnRefresh = new JButton("Refresh Active");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.add(btnRefresh);

        asgModel = nonEditableModel("ID","DriverID","VehicleID","StartDT","StartKM");
        asgTable = new JTable(asgModel);

        // create form
        cDriver = new JTextField("1");
        cVehicle = new JTextField("1");
        cStartKm = new JTextField("0");
        cStartDt = new JTextField(nowTsString());
        JButton btnCreate = new JButton("Create (SP)");

        JPanel createForm = labeledGrid(
                new String[]{"Driver ID","Vehicle ID","Start KM","Start DT (YYYY-MM-DD HH:MM:SS)"},
                new JComponent[]{cDriver, cVehicle, cStartKm, cStartDt}
        );
        JPanel createWrap = new JPanel(new BorderLayout(10, 10));
        createWrap.add(createForm, BorderLayout.CENTER);
        createWrap.add(btnCreate, BorderLayout.EAST);

        // close form
        clAsgId = new JTextField("");
        clEndKm = new JTextField("");
        clEndDt = new JTextField(nowTsString());
        JButton btnClose = new JButton("Close (SP)");

        JPanel closeForm = labeledGrid(
                new String[]{"Assignment ID","End KM","End DT (YYYY-MM-DD HH:MM:SS)"},
                new JComponent[]{clAsgId, clEndKm, clEndDt}
        );
        JPanel closeWrap = new JPanel(new BorderLayout(10, 10));
        closeWrap.add(closeForm, BorderLayout.CENTER);
        closeWrap.add(btnClose, BorderLayout.EAST);

        JPanel south = new JPanel(new GridLayout(2, 1, 10, 10));
        south.add(createWrap);
        south.add(closeWrap);

        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(asgTable), BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        btnRefresh.addActionListener(e -> refreshActiveAssignments());

        asgTable.getSelectionModel().addListSelectionListener(e -> {
            if (ignoreSelection(e)) return;
            int viewRow = asgTable.getSelectedRow();
            if (viewRow < 0) return;

            // JTable sorter varsa, view index -> model index çevir
            int modelRow = asgTable.convertRowIndexToModel(viewRow);

            clAsgId.setText(val(asgModel, modelRow, 0));

            // End KM default: StartKM + 10
            try {
                long skm = Long.parseLong(val(asgModel, modelRow, 4));
                clEndKm.setText(String.valueOf(skm + 10));
            } catch (Exception ignored) {}

            // EndDT default: şimdi; ama startDT'den önceyse startDT+1dk
            try {
                LocalDateTime start = parseStartDtFromTable(modelRow);
                LocalDateTime end = LocalDateTime.now().withNano(0);

                if (!end.isAfter(start)) {
                    end = start.plusMinutes(1);
                }
                clEndDt.setText(formatDt(end));
            } catch (Exception ex) {
                // fallback
                clEndDt.setText(nowTsString());
            }
        });

        btnCreate.addActionListener(e -> safe(() -> {
            long did = parseLong(cDriver, "Driver ID");
            long vid = parseLong(cVehicle, "Vehicle ID");
            long skm = parseLong(cStartKm, "Start KM");
            Timestamp sdt = Timestamp.valueOf(text(cStartDt));

            assignmentDAO.createAssignmentSP(did, vid, skm, sdt);

            info("Assignment created.");
            refreshActiveAssignmentMaps();
            refreshActiveAssignments();
            refreshVehicles();
            refreshDrivers();
        }));

        btnClose.addActionListener(e -> safe(() -> {
            if (text(clAsgId).isEmpty()) { info("Select an active assignment."); return; }

            long asgId = parseLong(clAsgId, "Assignment ID");
            long ekm = parseLong(clEndKm, "End KM");
            Timestamp edt = Timestamp.valueOf(text(clEndDt));

            int viewRow = asgTable.getSelectedRow();
            if (viewRow >= 0) {
                int modelRow = asgTable.convertRowIndexToModel(viewRow);
                LocalDateTime start = parseStartDtFromTable(modelRow);
                LocalDateTime end = edt.toLocalDateTime();
                if (!end.isAfter(start)) {
                    end = start.plusMinutes(1);
                    clEndDt.setText(formatDt(end));
                    edt = Timestamp.valueOf(clEndDt.getText().trim());
                }
            }

            assignmentDAO.closeAssignmentSP(asgId, ekm, edt);

            info("Assignment closed.");
            refreshActiveAssignmentMaps();
            refreshActiveAssignments();
            refreshVehicles();
            refreshDrivers();
        }));

        return root;
    }

    private void refreshActiveAssignments() {
        safe(() -> {
            asgModel.setRowCount(0);
            for (Assignment a : assignmentDAO.findActive()) {
                asgModel.addRow(new Object[]{
                        a.getAssignmentId(), a.getDriverId(), a.getVehicleId(),
                        a.getStartDatetime(), a.getStartKm()
                });
            }
        });
    }

    // =========================================================
    // REPORTS
    // =========================================================
    private JPanel buildReportsPanel() {
        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(220);
        splitPane.setDividerSize(2);
        splitPane.setBorder(null);

        JPanel menuPanel = new JPanel(new GridLayout(10, 1, 5, 5));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        menuPanel.setBackground(new Color(240, 240, 240));

        JLabel lblTitle = new JLabel("REPORT MENU", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(Color.GRAY);

        JButton btnActive = new JButton("Active Assignments");
        JButton btnMaintV1 = new JButton("Maintenance History (Veh#1)");
        JButton btnCost = new JButton("Cost Analysis");

        btnActive.setFocusable(false);
        btnMaintV1.setFocusable(false);
        btnCost.setFocusable(false);

        menuPanel.add(lblTitle);
        menuPanel.add(new JSeparator());
        menuPanel.add(btnActive);
        menuPanel.add(btnMaintV1);
        menuPanel.add(btnCost);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lblHeader = new JLabel("Select a report from the menu...", JLabel.LEFT);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));

        DefaultTableModel reportTableModel = new DefaultTableModel();
        JTable reportTable = new JTable(reportTableModel);
        reportTable.setFillsViewportHeight(true);
        reportTable.setRowHeight(30);
        reportTable.setShowVerticalLines(true);
        reportTable.setShowHorizontalLines(true);
        JScrollPane scrollPane = new JScrollPane(reportTable);

        contentPanel.add(lblHeader, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        btnActive.addActionListener(e -> {
            lblHeader.setText("Active Assignments Report");
            try {
                List<String> rows = reportDAO.activeAssignmentsDetailed();
                String[] columns = {"Asg ID", "Start Date", "Driver", "Vehicle Info"};
                updateReportTable(reportTableModel, columns, rows);
            } catch (Exception ex) { showError(ex); }
        });

        btnMaintV1.addActionListener(e -> {
            lblHeader.setText("Maintenance History: Vehicle #1");
            try {
                List<String> rows = reportDAO.maintenanceHistoryByVehicle(1);
                String[] columns = {"Maint ID", "Date", "Type", "Cost", "Desc"};
                updateReportTable(reportTableModel, columns, rows);
            } catch (Exception ex) { showError(ex); }
        });

        btnCost.addActionListener(e -> {
            lblHeader.setText("Total Maintenance Cost Per Vehicle");
            try {
                List<String> rows = reportDAO.totalMaintenanceCostPerVehicle();
                String[] columns = {"Vehicle", "Plate", "Maint Count", "Total Cost"};
                updateReportTable(reportTableModel, columns, rows);
            } catch (Exception ex) { showError(ex); }
        });

        splitPane.setLeftComponent(menuPanel);
        splitPane.setRightComponent(contentPanel);

        JPanel root = new JPanel(new BorderLayout());
        root.add(splitPane, BorderLayout.CENTER);
        return root;
    }

    private void updateReportTable(DefaultTableModel model, String[] columns, List<String> dataRows) {
        model.setRowCount(0);
        model.setColumnIdentifiers(columns);

        for (String rowString : dataRows) {
            if (rowString == null) continue;
            if (rowString.startsWith("=") || rowString.trim().isEmpty()) continue;
            if (rowString.equals("(none)")) continue;

            String[] parts = rowString.split("\\|");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }
            model.addRow(parts);
        }
    }

    private void showError(Exception ex) {
        err(ex);
    }

    // =========================================================
    // ACTIVE MAP REFRESH
    // =========================================================
    private void refreshActiveAssignmentMaps() {
        activeDriverNameByVehicle.clear();
        activeVehiclePlateByDriver.clear();

        String sql = "SELECT vehicle_id, driver_id, driver_name, plate_no " +
                "FROM vw_active_assignments_detailed";

        try (
                java.sql.Connection c = DBConnection.getConnection();
                java.sql.PreparedStatement ps = c.prepareStatement(sql);
                java.sql.ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                long vehicleId = rs.getLong("vehicle_id");
                long driverId  = rs.getLong("driver_id");
                String driverName = rs.getString("driver_name");
                String plateNo    = rs.getString("plate_no");

                activeDriverNameByVehicle.put(vehicleId, driverName);
                activeVehiclePlateByDriver.put(driverId, plateNo);
            }
        } catch (Exception ex) {
            err(ex);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private void applyUiDefaults() {
        try {
            FlatLightLaf.setup();
            Locale.setDefault(Locale.US);

            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.rowHeight", 26);
        } catch (Exception ignored) {}
    }

    private JPanel pad(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return p;
    }

    private JPanel searchBar(String label, JTextField field, JButton b1, JButton b2) {
        JPanel top = new JPanel(new BorderLayout(10, 10));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.add(b1);
        right.add(b2);
        top.add(new JLabel(label), BorderLayout.WEST);
        top.add(field, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);
        return top;
    }

    private JPanel labeledGrid(String[] labels, JComponent[] fields) {
        int n = labels.length;
        JPanel p = new JPanel(new GridLayout(2, n, 8, 8));
        for (String s : labels) p.add(new JLabel(s));
        for (JComponent c : fields) p.add(c);
        return p;
    }

    private DefaultTableModel nonEditableModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JTextField tf(boolean editable) {
        JTextField t = new JTextField();
        t.setEditable(editable);
        return t;
    }

    private boolean ignoreSelection(ListSelectionEvent e) { return e.getValueIsAdjusting(); }

    private String val(DefaultTableModel m, int r, int c) {
        Object o = m.getValueAt(r, c);
        return o == null ? "" : String.valueOf(o);
    }

    private String text(JTextField f) { return f.getText().trim(); }

    private int parseInt(JTextField f, String name) {
        String s = text(f);
        try { return Integer.parseInt(s); }
        catch (Exception e) { throw new IllegalArgumentException(name + " sayı olmalı: " + s); }
    }

    private long parseLong(JTextField f, String name) {
        String s = text(f);
        try { return Long.parseLong(s); }
        catch (Exception e) { throw new IllegalArgumentException(name + " sayı olmalı: " + s); }
    }

    private double parseDouble(JTextField f, String name) {
        String s = text(f);
        try { return Double.parseDouble(s); }
        catch (Exception e) { throw new IllegalArgumentException(name + " sayı olmalı: " + s); }
    }

    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    private void err(Exception ex) {
        ex.printStackTrace();

        String msg = null;
        Throwable t = ex;
        while (t != null) {
            if (t instanceof SQLException se) {
                if ("45000".equals(se.getSQLState())) { msg = se.getMessage(); break; }
                msg = se.getMessage();
            }
            t = t.getCause();
        }
        if (msg == null || msg.isBlank()) msg = ex.getMessage();
        if (msg == null || msg.isBlank()) msg = ex.getClass().getSimpleName();

        JOptionPane.showMessageDialog(this, msg, "Database / Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    private void safe(ThrowingRunnable r) {
        try { r.run(); }
        catch (Exception ex) { err(ex); }
    }

    private Assignment getActiveAssignmentForDriver(long driverId) throws Exception {
        for (Assignment a : assignmentDAO.findActive()) {
            if (a.getDriverId() == driverId) return a;
        }
        return null;
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }

    private String nowTsString() {
        return LocalDateTime.now().withNano(0).toString().replace('T', ' ');
    }

    private static class ComboItem {
        final long id;
        final String label;
        ComboItem(long id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return id + " - " + label; }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppUI().setVisible(true));
    }
}
