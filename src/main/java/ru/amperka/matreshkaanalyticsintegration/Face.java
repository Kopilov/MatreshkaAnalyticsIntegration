package ru.amperka.matreshkaanalyticsintegration;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.Action;
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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Визуальное окно для управления данными о веб-ресурсах
 * и тестирования обращений к Google Аналитике.
 * @author aleksandr
 */
public class Face extends javax.swing.JFrame {
	private static final Logger logger = Logger.getLogger(Face.class.getName());
	private static final ResourceBundle l10n = ResourceBundle.getBundle("ru.amperka.matreshkaanalyticsintegration.l10n");
	private JTabbedPane tabbedPane;
	private Component gridPanel, formPanel;
	JTable webresourcesTable;
	private List<JTextField> formTextFields = new ArrayList<>();
	//WebresourcesSwingTableModel webresourcesTableModel;
	
	public static void main(String[] args) {
		CommandLine commandLine;
		try {
			commandLine = parseCommandLine(args);
			List<String> argList = commandLine.getArgList();
			if (commandLine.hasOption("help") || argList.isEmpty()) {
				System.out.println(l10n.getString("detailedHelpGUI"));
				return;
			}
		} catch (ParseException ex) {
			System.out.println(ex.getLocalizedMessage());
			System.out.println(l10n.getString("help"));
			return;
		}
		open(commandLine);
	}
	
	/**
	 * Парсинг параметров командной строки с использованием Apache Commons CLI.
	 * Ищутся опции "login", "password", "help"
	 * @param args параметры командной строки
	 * @return
	 * @throws ParseException 
	 */
	private static CommandLine parseCommandLine(String[] args) throws ParseException {
		Options options = new Options();
		options.addOption(null, "login", true, "Database login");
		options.addOption(null, "password", true, "Database password");
		options.addOption("h", "help", false, "Show help message and exit");
		CommandLineParser parser = new DefaultParser();
		return parser.parse(options, args);
	}
	
	public static void open(CommandLine commandLine) {
		Face face = new Face(commandLine);
		face.initComponents();
		face.setVisible(true);
	}
	
	public static void open() {
		Face face = new Face();
		face.initComponents();
		face.setVisible(true);
	}
	
	public Face() {
		super("matreshkaanalyticsintegration");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	private Face(CommandLine commandLine) {
		this();
		String connectionString = commandLine.getArgList().get(0);
		String login = commandLine.getOptionValue("login");
		String password = commandLine.getOptionValue("password");
		ConnectionGate.getIstance().init(connectionString, login, password);
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
		final JMenu jMenu = new JMenu("aaas");
		menuBar.add(jMenu);
		jMenu.add(new JMenuItem("123"));
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
			tabbedPane.setSelectedComponent(formPanel);
		});
		buttonsPanel.add(addButton);
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
		JPanel formPanel = new JPanel(new BorderLayout());
		JPanel form = new JPanel(new SpringLayout());
		formPanel.add(form, BorderLayout.CENTER);
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
		formPanel.add(initFormToolbar(webresourcesTableModel), BorderLayout.SOUTH);
		return new JScrollPane(formPanel);
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
