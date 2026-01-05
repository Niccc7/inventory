/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.uas;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
/**
 *
 * @author NICHOLAS
 */
public class Inbound extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Inbound.class.getName());

    private int userId;
    private String username;
    private String role;
    
    private DefaultTableModel itemModel;
    
    public Inbound(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;

        initComponents();
        setupForm();
        setupEvents();
    }
    public Inbound() {
        initComponents();
        setupForm();
        setupEvents();
    }
    
    private void setupForm() {
        setLocationRelativeTo(null);

        type.removeAllItems();
        type.addItem("INBOUND");
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
        save.addActionListener(e -> saveInbound());
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

        // rapikan ulang line number
        for (int i = 0; i < itemModel.getRowCount(); i++) {
            itemModel.setValueAt(i + 1, i, 0);
        }
    }

    private void clearForm() {
        namesp.setText("");
        nosj.setText("");
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


    private void addStock(Connection conn, int productId, double qty) throws SQLException {
        String sql = "UPDATE stocks SET qty_on_hand = qty_on_hand + ?, updated_at = NOW() WHERE product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, qty);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    private void saveInbound() {
        if (namesp.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama Supplier wajib diisi!");
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

            // 1️⃣ INSERT HEADER
            String sqlTrx = """
                INSERT INTO inventory_transactions
                (trx_type, supplier_name, supplier_sj_no, trx_date, created_by, created_at, updated_at)
                VALUES (?, ?, ?, NOW(), ?, NOW(), NOW())
            """;

            psTrx = conn.prepareStatement(sqlTrx, Statement.RETURN_GENERATED_KEYS);
            psTrx.setString(1, "INBOUND");
            psTrx.setString(2, namesp.getText().trim());
            psTrx.setString(3, nosj.getText().trim());
            psTrx.setInt(4, userId);
            psTrx.executeUpdate();

            ResultSet rs = psTrx.getGeneratedKeys();
            int trxId = 0;
            if (rs.next()) trxId = rs.getInt(1);

            // 2️⃣ INSERT DETAIL ITEMS
            String sqlItem = """
                INSERT INTO inventory_transaction_items
                (trx_id, line_no, product_id, qty, direction)
                VALUES (?, ?, ?, ?, ?)
            """;

            psItem = conn.prepareStatement(sqlItem);

            for (int i = 0; i < itemModel.getRowCount(); i++) {
            int lineNo = Integer.parseInt(itemModel.getValueAt(i, 0).toString());

            String barcode = itemModel.getValueAt(i, 1).toString().trim();
            String qtyStr = itemModel.getValueAt(i, 2).toString().trim();

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

            // 🔥 lookup barcode -> product_id
            int productId = getProductIdByBarcode(conn, barcode);
            if (productId == -1) {
                throw new RuntimeException("Barcode tidak ditemukan: " + barcode + " (baris " + lineNo + ")");
            }

            // ✅ update stocks
            ensureStockRow(conn, productId);
            addStock(conn, productId, qty);

            // insert item (log)
            psItem.setInt(1, trxId);
            psItem.setInt(2, lineNo);
            psItem.setInt(3, productId);
            psItem.setDouble(4, qty);
            psItem.setString(5, "IN");
            psItem.addBatch();
        }
            psItem.executeBatch();
            conn.commit();

            JOptionPane.showMessageDialog(this, "Inbound berhasil disimpan!");
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

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        head = new javax.swing.JLabel();
        back = new javax.swing.JButton();
        notr1 = new javax.swing.JLabel();
        type = new javax.swing.JComboBox<>();
        notr2 = new javax.swing.JLabel();
        namesp = new javax.swing.JTextField();
        notr3 = new javax.swing.JLabel();
        nosj = new javax.swing.JTextField();
        notr4 = new javax.swing.JLabel();
        addi = new javax.swing.JButton();
        delit = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        tbitem = new javax.swing.JTable();
        clear = new javax.swing.JButton();
        save = new javax.swing.JButton();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(jTable2);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        head.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        head.setText("Inbound Transaction");

        back.setText("Back");
        back.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                backMouseClicked(evt);
            }
        });

        notr1.setText("Type Transaksi");

        type.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "INBOUND" }));

        notr2.setText("Nama Supplier");

        notr3.setText("No SJ Supplier");

        notr4.setText("Detail Items");

        addi.setText("Add item");

        delit.setText("Delete item");
        delit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delitActionPerformed(evt);
            }
        });

        jScrollPane3.setViewportView(tbitem);

        clear.setText("Clear");

        save.setText("Save");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(back)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(notr4, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(addi)
                        .addGap(18, 18, 18)
                        .addComponent(delit)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(100, 100, 100)
                                .addComponent(head, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(notr1, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(type, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(notr2, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(namesp))
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(notr3, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(nosj, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addContainerGap(92, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(clear)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(save)
                        .addGap(2, 2, 2))))
            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(back)
                .addGap(10, 10, 10)
                .addComponent(head)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(notr1)
                    .addComponent(type, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(notr2)
                    .addComponent(namesp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(notr3)
                    .addComponent(nosj, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(notr4)
                    .addComponent(addi)
                    .addComponent(delit))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 396, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(clear)
                    .addComponent(save))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

    private void delitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delitActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_delitActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new Inbound().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addi;
    private javax.swing.JButton back;
    private javax.swing.JButton clear;
    private javax.swing.JButton delit;
    private javax.swing.JLabel head;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField namesp;
    private javax.swing.JTextField nosj;
    private javax.swing.JLabel notr1;
    private javax.swing.JLabel notr2;
    private javax.swing.JLabel notr3;
    private javax.swing.JLabel notr4;
    private javax.swing.JButton save;
    private javax.swing.JTable tbitem;
    private javax.swing.JComboBox<String> type;
    // End of variables declaration//GEN-END:variables
}