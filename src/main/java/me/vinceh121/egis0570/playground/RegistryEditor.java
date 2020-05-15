package me.vinceh121.egis0570.playground;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import me.vinceh121.egis0570.Egis;

public class RegistryEditor extends JFrame {
	private static final long serialVersionUID = -4507059411518297827L;
	private final Egis egis;
	private final JTable table;

	public static void main(final String[] args) throws IOException {
		final RegistryEditor r = new RegistryEditor();
		r.setVisible(true);
	}

	public RegistryEditor() {
		this.egis = new Egis();
		this.egis.init();

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.table = new JTable();
		this.updateTable();
		this.add(new JScrollPane(this.table));

		final JMenuBar bar = new JMenuBar();
		this.setJMenuBar(bar);

		final JMenu mnFile = new JMenu("File");
		bar.add(mnFile);

		final JMenuItem mntOpen = new JMenuItem("Open");
		mnFile.add(mntOpen);

		final JMenuItem mntSave = new JMenuItem("Save");
		mntSave.addActionListener(e -> {
			final JFileChooser fc = new JFileChooser();
			final int status = fc.showSaveDialog(null);
			if (status != JFileChooser.APPROVE_OPTION)
				return;
			try {
				saveRegistry(new FileOutputStream(fc.getSelectedFile()));
			} catch (IOException e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(null, "Error while saving: " + e1.toString());
			}
		});
		mnFile.add(mntSave);
	}

	public void openRegistry(final InputStream in) throws IOException {
		final byte[] buf = new byte[2];
		while (in.read(buf) > 0) {
			egis.writeRegister(buf[0], buf[1]);
		}
		updateTable();
	}

	public void saveRegistry(final OutputStream out) throws IOException {
		final int[] sel = this.table.getSelectedRows();
		if (sel.length == 0) {
			for (int i = 0; i < Egis.REGISTRY_SIZE; i++) {
				out.write(i);
				out.write(this.egis.readRegister(i));
			}
		} else {
			for (final int i : sel) {
				out.write(i);
				out.write(this.egis.readRegister(i));
			}
		}
	}

	private void updateTable() {
		this.table.setModel(new RegistryModel(this.egis));
	}

	class RegistryModel extends AbstractTableModel {
		private static final long serialVersionUID = -5591790888978964019L;
		private final Egis egis;

		public RegistryModel(final Egis egis) {
			this.egis = egis;
		}

		@Override
		public int getRowCount() {
			return Egis.REGISTRY_SIZE;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex) {
			if (columnIndex == 0) {
				return "0x" + Integer.toHexString(rowIndex);
			} else if (columnIndex == 1) {
				return "0x" + Integer.toHexString(this.egis.readRegister(rowIndex));
			} else {
				return null;
			}
		}

	}
}
