/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.uas;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import javax.swing.*;

/**
 *
 * @author NICHOLAS
 */
public class Outbound extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Outbound.class.getName());

    private int userId;
    private String username;
    private String role;

    private DefaultTableModel itemModel;
    
    public Outbound(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;

        initComponents();
        setupForm();
        setupEvents();
    }
    
    public Outbound() {
        initComponents();
        setupForm();
        setupEvents();
    }
    
    private void setupForm() {
        setLocationRelativeTo(null);

        type.removeAllItems();
        type.addItem("OUTBOUND");
        type.setSelectedIndex(0);
        type.setEnabled(false);

        itemModel = new DefaultTableModel(
            new Object[]{"Line", "Barcode", "Qty"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col != 0;
            }
        };
        tbitem.setModel(itemModel);
    }

    private void setupEvents() {
        addi.addActionListener(e -> addItemRow());
        delit.addActionListener(e -> deleteSelectedRow());
        clear.addActionListener(e -> clearForm());
        save.addActionListener(e -> saveOutbound());
    }
    
    private void addItemRow() {
        int nextLine = itemModel.getRowCount() + 1;
        itemModel.addRow(new Object[]{nextLine, "", "0"});
    }

    private void deleteSelectedRow() {
        int row = tbitem.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih item yang ingin dihapus.");
            return;
        }
        itemModel.removeRow(row);

        for (int i = 0; i < itemModel.getRowCount(); i++) {
            itemModel.setValueAt(i + 1, i, 0);
        }
    }

    private void clearForm() {
        itemModel.setRowCount(0);
    }
    
    private int getProductIdByBarcode(Connection conn, String barcode) throws SQLException {
        String sql = "SELECT id FROM products WHERE code = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }

    private void ensureStockRow(Connection conn, int productId) throws SQLException {
        String sql = "INSERT INTO stocks (product_id, qty_on_hand, updated_at) " +
                     "VALUES (?, 0, NOW()) " +
                     "ON DUPLICATE KEY UPDATE updated_at = NOW()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }

    private double getStockForUpdate(Connection conn, int productId) throws SQLException {
        String sql = "SELECT qty_on_hand FROM stocks WHERE product_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0;
    }

    private void subtractStock(Connection conn, int productId, double qty) throws SQLException {
        String sql = "UPDATE stocks SET qty_on_hand = qty_on_hand - ?, updated_at = NOW() WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, qty);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }
    
    private String generateNoSuratJalan(int trxId) {
        java.time.LocalDate now = java.time.LocalDate.now();
        String tgl = now.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        return "SJ-" + tgl + "-" + trxId;
    }

    private void saveOutbound() {
        String buyerName = buyer.getText().trim();
        if (buyerName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Nama pembeli wajib diisi!",
                    "Validasi",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (itemModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Minimal 1 item harus ditambahkan!");
            return;
        }

        Connection conn = null;
        PreparedStatement psTrx = null;
        PreparedStatement psItem = null;

        try {
            conn = Koneksi.getConnection();
            conn.setAutoCommit(false);

            // 1️⃣ insert header
            String sqlTrx = """
                INSERT INTO inventory_transactions
                (trx_type, trx_date, buyer_name, created_by, created_at, updated_at)
                VALUES (?, NOW(), ?, ?, NOW(), NOW())
            """;
            psTrx = conn.prepareStatement(sqlTrx, Statement.RETURN_GENERATED_KEYS);
            psTrx.setString(1, "OUTBOUND");
            psTrx.setString(2, buyerName);
            psTrx.setInt(3, userId);
            psTrx.executeUpdate();

            int trxId = 0;
            try (ResultSet rs = psTrx.getGeneratedKeys()) {
                if (rs.next()) trxId = rs.getInt(1);
            }

            // 2️⃣ insert items
            String sqlItem = """
                INSERT INTO inventory_transaction_items
                (trx_id, line_no, product_id, qty, direction)
                VALUES (?, ?, ?, ?, ?)
            """;
            psItem = conn.prepareStatement(sqlItem);

            for (int i = 0; i < itemModel.getRowCount(); i++) {
                int lineNo = Integer.parseInt(itemModel.getValueAt(i, 0).toString());

                Object bcObj = itemModel.getValueAt(i, 1);
                Object qtyObj = itemModel.getValueAt(i, 2);

                String barcode = (bcObj == null) ? "" : bcObj.toString().trim();
                String qtyStr  = (qtyObj == null) ? "" : qtyObj.toString().trim();

                if (barcode.isEmpty()) {
                    throw new RuntimeException("Barcode di baris " + lineNo + " tidak boleh kosong.");
                }

                double qty;
                try {
                    qty = Double.parseDouble(qtyStr);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Qty di baris " + lineNo + " harus angka.");
                }

                if (qty <= 0) {
                    throw new RuntimeException("Qty di baris " + lineNo + " harus > 0");
                }

                // 🔥 lookup barcode → product_id
                int productId = getProductIdByBarcode(conn, barcode);
                if (productId == -1) {
                    throw new RuntimeException("Barcode tidak ditemukan: " + barcode);
                }

                // pastikan stok ada
                ensureStockRow(conn, productId);

                // 🔒 kunci stok & cek
                double onHand = getStockForUpdate(conn, productId);
                if (onHand < qty) {
                    throw new RuntimeException(
                        "Stok tidak cukup untuk barcode " + barcode +
                        " | Stok=" + onHand + " | Request=" + qty
                    );
                }

                // ➖ kurangi stok
                subtractStock(conn, productId, qty);

                // log transaksi OUT
                psItem.setInt(1, trxId);
                psItem.setInt(2, lineNo);
                psItem.setInt(3, productId);
                psItem.setDouble(4, qty);
                psItem.setString(5, "OUT");
                psItem.addBatch();
            }

            psItem.executeBatch();
            conn.commit();

            JOptionPane.showMessageDialog(this, "Outbound berhasil disimpan!");
            
            int pilih = JOptionPane.showConfirmDialog(
                    this,
                    "Apakah barang ini akan dikirim?",
                    "Konfirmasi Pengiriman",
                    JOptionPane.YES_NO_OPTION
            );

            if (pilih == JOptionPane.YES_OPTION) {
                String noSJ = generateNoSuratJalan(trxId);
                Surat_Jalan sj = new Surat_Jalan(trxId, noSJ, userId, buyerName);
                sj.setVisible(true);
            }

            clearForm();
        } catch (Exception ex) {
            try { if (conn != null) conn.rollback(); } catch (Exception ignored) {}
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } finally {
            try { if (psItem != null) psItem.close(); } catch (Exception ignored) {}
            try { if (psTrx != null) psTrx.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        back = new javax.swing.JButton();
        head = new javax.swing.JLabel();
        tys = new javax.swing.JLabel();
        type = new javax.swing.JComboBox<>();
        detail = new javax.swing.JLabel();
        addi = new javax.swing.JButton();
        delit = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbitem = new javax.swing.JTable();
        save = new javax.swing.JButton();
        clear = new javax.swing.JButton();
        tys1 = new javax.swing.JLabel();
        buyer = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        back.setText("Back");
        back.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                backMouseClicked(evt);
            }
        });

        head.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        head.setText("Outbound Transaction");

        tys.setText("Type Transaksi");

        type.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "OUTBOUND" }));

        detail.setText("Detail Items");

        addi.setText("Add Item");

        delit.setText("Delete Item");

        jScrollPane1.setViewportView(tbitem);

        save.setText("Save");

        clear.setText("Clear");

        tys1.setText("Nama Pembeli");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(back)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(clear)
                .addGap(18, 18, 18)
                .addComponent(save))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(detail, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(97, 97, 97)
                        .addComponent(addi)
                        .addGap(18, 18, 18)
                        .addComponent(delit))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(89, 89, 89)
                        .addComponent(head))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(tys1, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(buyer))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addComponent(tys, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(type, javax.swing.GroupLayout.PREFERRED_SIZE, 212, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(8, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(back)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(head)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tys)
                    .addComponent(type, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tys1, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(buyer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(detail)
                    .addComponent(addi)
                    .addComponent(delit))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(save)
                    .addComponent(clear)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void backMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_backMouseClicked
        // TODO add your handling code here:
        this.setVisible(false);
        
        if ("ADMIN".equalsIgnoreCase(role)) {
            new Main_Admin(userId, username, role).setVisible(true);
        } else {
            new Main_Head(userId, username, role).setVisible(true);
        }
    }//GEN-LAST:event_backMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new Outbound().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addi;
    private javax.swing.JButton back;
    private javax.swing.JTextField buyer;
    private javax.swing.JButton clear;
    private javax.swing.JButton delit;
    private javax.swing.JLabel detail;
    private javax.swing.JLabel head;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton save;
    private javax.swing.JTable tbitem;
    private javax.swing.JComboBox<String> type;
    private javax.swing.JLabel tys;
    private javax.swing.JLabel tys1;
    // End of variables declaration//GEN-END:variables
}
