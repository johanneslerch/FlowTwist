package flow.twist.debugger;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import flow.twist.debugger.Debugger.DebuggerListener;

public class TabularViewer {

	private DebuggerListener listener;
	private DefaultTableModel tableModel;
	private Object[][] data;
	private JButton pauseButton;
	private boolean paused = false;
	private Debugger debugger;
	private JFrame frame;

	public TabularViewer(final Debugger debugger, final DebuggerListener listener) {
		this.debugger = debugger;
		this.listener = listener;

		frame = new JFrame();

		pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (paused) {
					paused = false;
					debugger.play();
					pauseButton.setText("Pause");
				} else {
					paused = true;
					debugger.pause();
					pauseButton.setText("Play");

					data = listener.getData();
					tableModel.setDataVector(data, listener.getHeader());
				}
			}
		});
		frame.add(pauseButton, BorderLayout.NORTH);

		tableModel = new DefaultTableModel();
		tableModel.setColumnIdentifiers(listener.getHeader());
		final JTable table = new JTable(tableModel);
		final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(tableModel) {
			@Override
			public Comparator<?> getComparator(int column) {
				return new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						if (o1.matches("\\d+") && o2.matches("\\d+")) {
							return new Integer(Integer.parseInt(o1)).compareTo(Integer.parseInt(o2));
						} else
							return o1.compareTo(o2);
					}
				};
			}
		};
		table.setRowSorter(sorter);
		frame.add(new JScrollPane(table));
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.setSize(400, 600);
		frame.setVisible(true);

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				listener.debugCallback(data, sorter.convertRowIndexToModel(row), table.columnAtPoint(e.getPoint()));
			}
		});
	}

	public void dispose() {
		frame.setVisible(false);
		frame.dispose();
	}
}
