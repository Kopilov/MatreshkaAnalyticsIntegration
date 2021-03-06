package ru.ardu_cris.mai.face;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpringLayout;
import javax.swing.table.JTableHeader;
import org.apache.commons.lang3.StringEscapeUtils;
import ru.ardu_cris.mai.ConnectionGate;
import ru.ardu_cris.mai.daemon.Daemon;

/**
 * Визуальное окно для управления данными о веб-ресурсах
 * и тестирования обращений к Google Аналитике.
 * @author aleksandr
 */
public class Face extends javax.swing.JFrame {
	private static final Logger logger = Logger.getLogger(Face.class.getName());
	private FaceModule faceModule;
	private static final ResourceBundle l10n = ResourceBundle.getBundle("ru.ardu_cris.mai.l10n");
	private JTabbedPane tabbedPane;
	private Component gridPanel, formPanel;
	JTable webresourcesTable;
	private List<JTextField> formTextFields = new ArrayList<>();
	//WebresourcesSwingTableModel webresourcesTableModel;
	
	public Face(FaceModule faceModule, boolean startedFromDaemon) {
		super("matreshkaanalyticsintegration");
//		if (startedFromDaemon) {
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//		} else {
//			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		}
		this.faceModule = faceModule;
		initComponents();
	}
	
	private void initComponents() {
		readStoredPreferredSize();
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				writeStoredPreferredSize();
			}
		});
		JMenuBar menuBar = new JMenuBar();
		final JMenu jMenu = new JMenu(l10n.getString("Menu"));
		menuBar.add(jMenu);
		JMenuItem startDaemonItem = new JMenuItem(l10n.getString("Start background process"));
		startDaemonItem.addActionListener((ActionEvent e) -> {
			Daemon daemon = new Daemon(true);
			daemon.execute(faceModule.getCommandLine());
		});
		jMenu.add(startDaemonItem);
		this.setJMenuBar(menuBar);
		tabbedPane = new JTabbedPane();
		try {
			WebresourcesSwingTableModel webresourcesTableModel = new WebresourcesSwingTableModel(ConnectionGate.getIstance().getConnection());
			webresourcesTableModel.refresh();
			gridPanel = initGridPanel(webresourcesTableModel);
			tabbedPane.add(l10n.getString("webresourcesList"), gridPanel);
			formPanel = initFormPanel(webresourcesTableModel);
			tabbedPane.add(l10n.getString("webresourceDetails"), formPanel);
		} catch (SQLException ex) {
			logger.log(Level.WARNING, null, ex);
			showErrorPanel(ex);
		}
		this.add(tabbedPane);
	}
	
	/**
	 * Создание вкладки с таблицей, отображающей содержимое БД
	 * @return
	 * @throws SQLException 
	 */
	private JPanel initGridPanel(WebresourcesSwingTableModel webresourcesTableModel) throws SQLException {
		webresourcesTable = new JTable(){
			@Override
			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {
					@Override
					public String getToolTipText(MouseEvent e) {
						java.awt.Point p = e.getPoint();
						int index = columnModel.getColumnIndexAtX(p.x);
						int realIndex = columnModel.getColumn(index).getModelIndex();
						
						return webresourcesTableModel.getColumnTip(realIndex);
					}
				};
			}
		};
		webresourcesTable.setModel(webresourcesTableModel);
		JScrollPane tablePane = new JScrollPane(webresourcesTable);
		JPanel gridPanel = new JPanel();
		gridPanel.setLayout(new BorderLayout());
		gridPanel.add(tablePane);
		gridPanel.add(initGridToolbar(webresourcesTableModel), BorderLayout.SOUTH);
		return gridPanel;
	}
	
	private Component initGridToolbar(WebresourcesSwingTableModel webresourcesTableModel) {
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton addButton = new JButton(l10n.getString("addWebresourceButton"));
		addButton.addActionListener((ActionEvent e) -> {
			clearForm(formTextFields);
			tabbedPane.setSelectedComponent(formPanel);
		});
		buttonsPanel.add(addButton);
		JButton editButton = new JButton(l10n.getString("editWebresourceButton"));
		editButton.addActionListener((ActionEvent e) -> {
			clearForm(formTextFields);
			tabbedPane.setSelectedComponent(formPanel);
			try {
				webresourcesTableModel.loadWebresourceToForm(formTextFields, webresourcesTable.getSelectedRow());
			} catch (SQLException ex) {
				Logger.getLogger(Face.class.getName()).log(Level.SEVERE, null, ex);
			}
		});
		buttonsPanel.add(editButton);
		JButton removeButton = new JButton(l10n.getString("removeWebresourceButton"));
		removeButton.addActionListener((ActionEvent e) -> {
			int[] selectedRows = webresourcesTable.getSelectedRows();
			if (selectedRows.length == 0) {
				return;
			}
			int confirmed = JOptionPane.showConfirmDialog(
					Face.this,
					l10n.getString("deleteConfirmLabel"),
					l10n.getString("deleteConfirmTitle"),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE
			);
			if (confirmed == 0) {
				webresourcesTableModel.removeRows(selectedRows);
			}
		});
		buttonsPanel.add(removeButton);
		return buttonsPanel;
	}
	
	/**
	 * Создание вкладки с формой для редактирования записи
	 * @return 
	 */
	private Component initFormPanel(WebresourcesSwingTableModel webresourcesTableModel) {
		JPanel form = new JPanel(new SpringLayout());
		int numberOfFields = webresourcesTableModel.getColumnCount();
		for (int i = 0 ; i < numberOfFields; i++) {
			JLabel label = new JLabel(webresourcesTableModel.getColumnDescription(i), JLabel.TRAILING);
			form.add(label);
			JTextField textField = new JTextField(10);
			textField.setEnabled(i > 0);
			label.setLabelFor(textField);
			formTextFields.add(textField);
			form.add(textField);
		}
		SpringLayoutUtilities.makeCompactGrid(form,
		                                numberOfFields, 2, //rows, cols
		                                6, 6,        //initX, initY
		                                6, 6);       //xPad, yPad
		JPanel formPanelInScroll = new JPanel(new BorderLayout());
		formPanelInScroll.add(form, BorderLayout.CENTER);
		formPanelInScroll.add(initFormToolbar(webresourcesTableModel), BorderLayout.SOUTH);
		return new JScrollPane(formPanelInScroll);
	}
	
	void clearForm(Collection<JTextField> formTextFields) {
		for (JTextField formTextField: formTextFields) {
			formTextField.setText("");
		}
	}
	
	private Component initFormToolbar(WebresourcesSwingTableModel webresourcesTableModel) {
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton saveButton = new JButton(l10n.getString("save"));
		toolbar.add(saveButton);
		saveButton.addActionListener((ActionEvent e) -> {
			try {
				webresourcesTableModel.saveWebresourceFromForm(formTextFields);
				tabbedPane.setSelectedComponent(gridPanel);
			} catch (Exception ex) {
				logger.log(Level.WARNING, null, ex);
				showErrorPanel(ex);
			}
		});
		JButton cancelButton = new JButton(l10n.getString("cancel"));
		toolbar.add(cancelButton);
		cancelButton.addActionListener((ActionEvent e) -> {
			tabbedPane.setSelectedComponent(gridPanel);
		});
		return toolbar;
	}
	/**
	 * Чтение пользовательских настроек о состоянии окна
	 */
	private void readStoredPreferredSize() {
		Preferences preferences = Preferences.userNodeForPackage(Face.class);
		int frameWidth = preferences.getInt("frameWidth", 800);
		int frameHeight = preferences.getInt("frameHeight", 600);
		int frameLeft = preferences.getInt("frameLeft", 100);
		int frameTop = preferences.getInt("frameTop", 50);
		int frameState = preferences.getInt("frameState", JFrame.NORMAL);
		if (frameState == JFrame.MAXIMIZED_BOTH) {
			this.setExtendedState(frameState);
			this.setSize(new Dimension(frameWidth - 200, frameHeight - 100));
			setLocation(100, 50);
		} else {
			setSize(frameWidth, frameHeight);
			setLocation(frameLeft, frameTop);
		}
	}
	
	/**
	 * Запись пользовательских настроек о состоянии окна
	 */
	private void writeStoredPreferredSize() {
		Preferences preferences = Preferences.userNodeForPackage(Face.class);
		preferences.putInt("frameWidth", getWidth());
		preferences.putInt("frameHeight", getHeight());
		preferences.putInt("frameLeft", getX());
		preferences.putInt("frameTop", getY());
		preferences.putInt("frameState", getExtendedState());
	}
	
	/**
	 * Display error on tabbed pane
	 * @param ex exception to display
	 */
	private void showErrorPanel(Exception ex) {
		//prapare tab for error display
		JPanel errorPanel = new JPanel();
		errorPanel.setLayout(new BoxLayout(errorPanel, BoxLayout.Y_AXIS));
		logger.info("addErrorTab");
		//prepare main message
		StringBuilder message =  new StringBuilder("<html>");
		message.append("<font color=red size=+1>");
		message.append(l10n.getString("operationFailed"));
		message.append("</font><br/>");
		message.append(ex.getMessage());
		//add label with main message
		JLabel errorLabel = new JLabel();
		errorLabel.setText(message.toString());
		errorPanel.add(errorLabel);
		//add button to close
		Dimension space = new Dimension(100, 10);
		errorPanel.add(new Box.Filler(space, space, space));
		JButton closeButton = new JButton(l10n.getString("close"));
		errorPanel.add(closeButton);
		//add toggle button to display details
		errorPanel.add(new Box.Filler(space, space, space));
		JToggleButton detailsButton = new JToggleButton(l10n.getString("showDetails"));
		errorPanel.add(detailsButton);
		errorPanel.add(new Box.Filler(space, space, space));
		//prepare details message
		StringBuilder details = new StringBuilder("<html><pre>");
		StringWriter errors = new StringWriter();
		ex.printStackTrace(new PrintWriter(errors));
		details.append(StringEscapeUtils.escapeHtml4(errors.toString()));
		//add scrollable text with details
		JEditorPane detailsText = new JEditorPane("text/html", details.toString());
		JScrollPane detailsTextScroll = new JScrollPane(detailsText);
		errorPanel.add(detailsTextScroll);
		//hide detaild by default, toggle with button
		detailsTextScroll.setVisible(false);
		detailsButton.addActionListener((ActionEvent e) -> {
			detailsTextScroll.setVisible(detailsButton.isSelected());
			Face.this.repaint();
			detailsTextScroll.repaint();
		});
		//tab display manage
		tabbedPane.add("Error", errorPanel);
		tabbedPane.setSelectedComponent(errorPanel);
		closeButton.addActionListener((ActionEvent e) -> {
			tabbedPane.remove(errorPanel);
		});
	}
}
