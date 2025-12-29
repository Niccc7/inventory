/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.uas;
import java.sql.*;
import javax.swing.JOptionPane;

/**
 *
 * @author NICHOLAS
 */
public class Stok_Adj extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Stok_Adj.class.getName());

    private int userId;
    private String role;
    private String username;
    
    public Stok_Adj(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;

        initComponents();
        setLocationRelativeTo(null);
        loadProductCode();
    }

    public Stok_Adj(int userId) {
        this(userId, null, null);
    }

    public Stok_Adj() {
        this(0, null, null);
    }

    
    private static class ProductItem {
        private int id;
        private String code;

        ProductItem(int id, String code) {
            this.id = id;
            this.code = code;
        }

        int getId() {
            return id;
        }

        @Override
        public String toString() {
            return code;
        }
    }

    private void loadProductCode() {
        prid.removeAllItems();

        String sql = "SELECT id, code FROM products ORDER BY code ASC";

        try (Connection conn = Koneksi.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String code = rs.getString("code");
                prid.addItem(new ProductItem(id, code));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Gagal load product code: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        index = new javax.swing.JButton();
        head = new javax.swing.JLabel();
        pc = new javax.swing.JLabel();
        quantity = new javax.swing.JLabel();
        qty = new javax.swing.JTextField();
        quantity1 = new javax.swing.JLabel();
        reason = new javax.swing.JTextField();
        clear = new javax.swing.JButton();
        save = new javax.swing.JButton();
        prid = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        index.setText("Back to Index");
        index.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                indexMouseClicked(evt);
            }
        });

        head.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        head.setText("Stock Adjustment");

        pc.setText("Produk Code");

        quantity.setText("QTY");

        quantity1.setText("Alasan");

        clear.setText("Clear");
        clear.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                clearMouseClicked(evt);
            }
        });

        save.setText("Save");
        save.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                saveMouseClicked(evt);
            }
        });

        prid.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(index)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(pc, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(prid, 0, 185, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(85, 85, 85)
                        .addComponent(head))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(quantity, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(qty, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(quantity1, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(reason))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addComponent(clear)
                        .addGap(47, 47, 47)
                        .addComponent(save)))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(index)
                .addGap(9, 9, 9)
                .addComponent(head)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pc)
                    .addComponent(prid, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(quantity)
                    .addComponent(qty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(quantity1)
                    .addComponent(reason, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(clear)
                    .addComponent(save))
                .addGap(0, 19, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void indexMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_indexMouseClicked
        // TODO add your handling code here:
        this.setVisible(false);

        if ("ADMIN".equalsIgnoreCase(role)) {
            new Main_Admin(userId, username, role).setVisible(true);
        } else {
            new Main_Head(userId, username, role).setVisible(true);
        }
    }//GEN-LAST:event_indexMouseClicked

    private void clearMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clearMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_clearMouseClicked

    private void saveMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_saveMouseClicked
        // TODO add your handling code here:
        Object selected = prid.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Pilih product code dulu!");
            return;
        }
        ProductItem p = (ProductItem) selected;
        int productId = p.getId();

        // ambil input qty & alasan
        String qtyText = qty.getText().trim();
        String alasan = reason.getText().trim();

        if (qtyText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "QTY wajib diisi!");
            return;
        }
        if (alasan.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Alasan wajib diisi!");
            return;
        }

        java.math.BigDecimal qtyDiff;
        try {
            qtyDiff = new java.math.BigDecimal(qtyText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "QTY harus angka (contoh: 10 atau 2.5)!");
            return;
        }

        if (userId <= 0) {
            JOptionPane.showMessageDialog(this, "Session login tidak valid. Silakan login ulang!");
            return;
        }
        int createdBy = userId;
        if (createdBy <= 0) {
            JOptionPane.showMessageDialog(this, "User login tidak valid (created_by kosong).");
            return;
        }

        final String SQL_INSERT_ADJ =
                "INSERT INTO stock_adjustments (product_id, qty_diff, reason, created_by) " +
                "VALUES (?, ?, ?, ?)";
        
        final String SQL_CHECK_STOCK =
                "SELECT id FROM stocks WHERE product_id = ?";

        final String SQL_GET_STOCK =
                "SELECT qty_on_hand FROM stocks WHERE product_id = ?";

        final String SQL_INSERT_STOCK =
                "INSERT INTO stocks (product_id, qty_on_hand) VALUES (?, ?)";

        final String SQL_UPDATE_STOCK =
                "UPDATE stocks " +
                "SET qty_on_hand = qty_on_hand + ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE product_id = ?";

        Connection conn = null;

        try {
            conn = Koneksi.getConnection();
            conn.setAutoCommit(false);

            // Insert ke stock_adjustments 
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_ADJ)) {
                ps.setInt(1, productId);
                ps.setBigDecimal(2, qtyDiff);
                ps.setString(3, alasan);
                ps.setInt(4, createdBy);
                ps.executeUpdate();
            }

            // cek apakah stok sudah ada
            boolean stockExists = false;
            try (PreparedStatement ps = conn.prepareStatement(SQL_CHECK_STOCK)) {
                ps.setInt(1, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    stockExists = rs.next();
                }
            }

            // VALIDASI: kalau qtyDiff negatif, stok tidak boleh minus
            if (qtyDiff.compareTo(java.math.BigDecimal.ZERO) < 0) {

                // kalau stok belum ada tapi mau pengurangan -> pasti minus
                if (!stockExists) {
                    throw new Exception("Stok masih 0, tidak bisa dikurangi (" + qtyDiff + ")");
                }

                java.math.BigDecimal currentStock = java.math.BigDecimal.ZERO;
                try (PreparedStatement ps = conn.prepareStatement(SQL_GET_STOCK)) {
                    ps.setInt(1, productId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            currentStock = rs.getBigDecimal("qty_on_hand");
                            if (currentStock == null) currentStock = java.math.BigDecimal.ZERO;
                        }
                    }
                }

                java.math.BigDecimal newStock = currentStock.add(qtyDiff); // qtyDiff negatif
                if (newStock.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    throw new Exception("Stok tidak cukup! Stok sekarang: " + currentStock + ", pengurangan: " + qtyDiff);
                }
            }

            if (!stockExists) {
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_STOCK)) {
                    ps.setInt(1, productId);
                    ps.setBigDecimal(2, qtyDiff);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STOCK)) {
                    ps.setBigDecimal(1, qtyDiff);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                }
            }

            conn.commit();

            JOptionPane.showMessageDialog(this,
                    "Stock adjustment berhasil disimpan & stok ter-update!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            this.setVisible(false);
            new Main_Stok(userId, username, role).setVisible(true);

        } catch (Exception e) {
            // rollback kalau ada error
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            JOptionPane.showMessageDialog(this,
                    "Gagal simpan stock adjustment:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);

        }
    }//GEN-LAST:event_saveMouseClicked

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
        java.awt.EventQueue.invokeLater(() -> new Stok_Adj().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clear;
    private javax.swing.JLabel head;
    private javax.swing.JButton index;
    private javax.swing.JComboBox prid;
    private javax.swing.JLabel pc;
    private javax.swing.JTextField qty;
    private javax.swing.JLabel quantity;
    private javax.swing.JLabel quantity1;
    private javax.swing.JTextField reason;
    private javax.swing.JButton save;
    // End of variables declaration//GEN-END:variables
}
