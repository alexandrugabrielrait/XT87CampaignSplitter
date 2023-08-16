package net.xetanth87.campaignsplitter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static net.xetanth87.campaignsplitter.XT87Utils.getLocalizedString;

public class SplitterFrame extends JFrame {
	public static final String AUTHOR = "Xetanth87";
	public static final String APP_TITLE = "Campaign Splitter";
	private final JTextField filePathField = new JTextField(30);
	private final JButton browse = new JButton(getLocalizedString("button_open"));
	private final JButton split = new JButton(getLocalizedString("button_split"));
	private boolean running = false;
	private Thread splitterThread = null;
	private final JProgressBar bar;
	private final JTextArea taskOutput;
	private XT87Utils.TriOption difficultySelectorOption = XT87Utils.TriOption.SMART;
	private final ButtonGroup difficultySelectorGroup;
	private final JCheckBox campaignPreviewCheck = new JCheckBox();
	private final JCheckBox nextLevelCheck = new JCheckBox();
	private final JCheckBox legacyAssetsCheck = new JCheckBox();
	private final JCheckBox upkeepRemovalCheck = new JCheckBox();
	private final JCheckBox toDownloadFolderCheck = new JCheckBox();
	private final JComboBox<ArchonModeAddedSlotsVariant> archonModeAddedSlotsDropdown = new JComboBox<>();
	private final List<? extends JComponent> componentList = Arrays.asList(browse, campaignPreviewCheck, nextLevelCheck, legacyAssetsCheck, upkeepRemovalCheck, toDownloadFolderCheck);

	private static class ArchonModeAddedSlotsVariant {
		private final int value;

		private ArchonModeAddedSlotsVariant(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public String toString() {
			if (value == 0)
				return "Singleplayer";
			return (value + 1) + " players";
		}
	}

	public SplitterFrame() {
		super(AUTHOR + "'s " + APP_TITLE);
		setSize(600, 580);
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLayout(new FlowLayout());

		JPanel panel = new JPanel(new GridLayout(0, 1));
		add(panel);
		panel.add(new Label("Click \"" + browse.getText() + "\" to select a Warcraft III Custom Campaign file (\"*.w3n\").", Label.CENTER));
		panel.add(new Label("Click \"" + split.getText() + "\" to split the campaign (the campaign file will not be altered).", Label.CENTER));
		panel.add(new Label("A folder with the same name as the campaign will be created in the same location.", Label.CENTER));
		panel.add(new Label("This folder will contain all extracted maps, merged with campaign data.", Label.CENTER));

		JPanel optionsPanel = new JPanel(new GridLayout(0, 1));
		add(optionsPanel);

		panel = new JPanel();
		optionsPanel.add(panel);
		panel.add(new JLabel(getLocalizedString("difficulty_selector_check")));
		difficultySelectorGroup = new ButtonGroup();
		JRadioButton radioButton;
		for (XT87Utils.TriOption option : XT87Utils.TriOption.values()) {
			String label = "difficulty_selector_" + option.toString().toLowerCase(Locale.ROOT);
			radioButton = new JRadioButton(getLocalizedString(label));
			radioButton.setFocusPainted(false);
			radioButton.setActionCommand(option.toString());
			if (option == difficultySelectorOption)
				radioButton.setSelected(true);
			radioButton.setToolTipText(getLocalizedString(label + "_tooltip"));
			radioButton.addActionListener(new DifficultySelectorOptionL(option));
			panel.add(radioButton);
			difficultySelectorGroup.add(radioButton);
		}

		String label;
		panel = new JPanel();
		optionsPanel.add(panel);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		label = "campaign_preview_check";
		panel.add(new JLabel(getLocalizedString(label)));
		campaignPreviewCheck.setSelected(true);
		panel.add(campaignPreviewCheck);
		campaignPreviewCheck.setToolTipText(getLocalizedString(label + "_tooltip"));
		panel.add(new JLabel(getLocalizedString("next_level_name_check")));
		nextLevelCheck.setSelected(true);
		panel.add(nextLevelCheck);
		nextLevelCheck.setToolTipText(getLocalizedString(label + "_tooltip"));

		panel = new JPanel();
		optionsPanel.add(panel);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		label = "legacy_assets_check";
		panel.add(new JLabel(getLocalizedString(label)));
		legacyAssetsCheck.setSelected(true);
		panel.add(legacyAssetsCheck);
		legacyAssetsCheck.setToolTipText(getLocalizedString(label + "_tooltip"));
		label = "remove_upkeep_check";
		panel.add(new JLabel(getLocalizedString(label)));
		upkeepRemovalCheck.setSelected(false);
		panel.add(upkeepRemovalCheck);
		upkeepRemovalCheck.setToolTipText(getLocalizedString(label + "_tooltip"));

		panel = new JPanel();
		optionsPanel.add(panel);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		label = "archon_mode_dropdown";
		panel.add(new JLabel(getLocalizedString(label)));
		panel.add(archonModeAddedSlotsDropdown);
		for (int i = 0; i < 24; i++)
			archonModeAddedSlotsDropdown.addItem(new ArchonModeAddedSlotsVariant(i));
		archonModeAddedSlotsDropdown.setToolTipText(getLocalizedString(label + "_tooltip"));
		archonModeAddedSlotsDropdown.addItemListener(e -> {
			enableDisableToDownloadFolderCheck();
		});
		label = "to_download_folder_check";
		panel.add(new JLabel(getLocalizedString(label)));
		toDownloadFolderCheck.setSelected(false);
		panel.add(toDownloadFolderCheck);
		toDownloadFolderCheck.setToolTipText(getLocalizedString(label + "_tooltip"));
		enableDisableToDownloadFolderCheck();

		browse.addActionListener(this::openAction);
		split.addActionListener(this::splitAction);
		panel = new JPanel();
		add(panel);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		panel.add(filePathField);
		panel.add(browse);
		browse.setFocusPainted(false);
		browse.setBackground(new Color(100, 200, 255));
		panel.add(split);
		split.setFocusPainted(false);
		split.setBackground(Color.ORANGE);

		panel = new JPanel();
		add(panel);
		bar = new JProgressBar();
		bar.setValue(0);
		bar.setMaximum(100);
		bar.setStringPainted(true);
		bar.setPreferredSize(new Dimension((int) (0.9f * getWidth()), bar.getPreferredSize().height));
		panel.add(bar);

		panel = new JPanel();
		add(panel);
		taskOutput = new JTextArea(12, 50);
		taskOutput.setMargin(new Insets(5, 5, 5, 5));
		taskOutput.setEditable(false);
		panel.add(new JScrollPane(taskOutput), BorderLayout.CENTER);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getResource("/Icon.png"))).getImage());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JMenu menu;
		JMenuItem menuItem;
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		menu = new JMenu("File");
		menu.setMnemonic('F');
		menuBar.add(menu);

		menuItem = new JMenuItem("Open Campaign...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke('O', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(this::openAction));
		menu.add(menuItem);

		menuItem = new JMenuItem("Open from Maps folder");
		menuItem.setAccelerator(KeyStroke.getKeyStroke('D', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e -> openAction(e, XT87Utils.MAPS_FOLDER)));
		menu.add(menuItem);

		menuItem = new JMenuItem("Open from Campaigns folder");
		menuItem.setAccelerator(KeyStroke.getKeyStroke('F', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e -> openAction(e, XT87Utils.CAMPAIGNS_FOLDER)));
		menu.add(menuItem);

		menuItem = new JMenuItem("Clear");
		menuItem.setAccelerator(KeyStroke.getKeyStroke('W', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(this::clearAction));
		menu.add(menuItem);

		menuItem = new JMenuItem("Preferences");
		menuItem.setAccelerator(KeyStroke.getKeyStroke('P', KeyEvent.CTRL_DOWN_MASK));
		//menuItem.addActionListener(new ActionListenerInPopup(this::preferencesAction));
		menu.add(menuItem);

		menuItem = new JMenuItem("Exit");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));
		menuItem.addActionListener(e -> System.exit(0));
		menu.add(menuItem);

		menu = new JMenu("Edit");
		menu.setMnemonic('E');
		menuBar.add(menu);

		menuItem = new JMenuItem("Split/Stop");
		menuItem.setAccelerator(KeyStroke.getKeyStroke('S', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(this::splitAction));
		menu.add(menuItem);

		menu = new JMenu("Help");
		menu.setMnemonic('H');
		menuBar.add(menu);

		menu = new JMenu("Donate");
		menu.setMnemonic('H');
		menuBar.add(menu);

		menuItem = new JMenuItem("Become a Patron");
		menuItem.setAccelerator(KeyStroke.getKeyStroke('T', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e -> {
			try {
				Desktop.getDesktop().browse(new URI("https://www.patreon.com/alternatecampaigns")); // Open URL in browser
			} catch (IOException | URISyntaxException ex) {
				ex.printStackTrace();
			}
		}));
		menu.add(menuItem);

		menuItem = new JMenuItem("One Time Donation");
		menuItem.setAccelerator(KeyStroke.getKeyStroke('T', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e -> {
			try {
				Desktop.getDesktop().browse(new URI("https://www.buymeacoffee.com/xetanth87")); // Open URL in browser
			} catch (IOException | URISyntaxException ex) {
				ex.printStackTrace();
			}
		}));
		menu.add(menuItem);

		setVisible(true);
	}

	class DifficultySelectorOptionL implements ActionListener {
		private XT87Utils.TriOption option;

		public DifficultySelectorOptionL(XT87Utils.TriOption option) {
			this.option = option;
		}

		public void actionPerformed(ActionEvent e) {
			difficultySelectorOption = option;
		}
	}

	public void openAction(ActionEvent e, String overridePath) {
		if (running)
			return;
		JFileChooser c = new JFileChooser();
		try {
			File directory;
			if (overridePath == null) {
				if (filePathField.getText().isEmpty())
					directory = new File(XT87Utils.GAME_FOLDER);
				else
					directory = new File(filePathField.getText());
			} else
				directory = new File(overridePath);
			if (!directory.isDirectory())
				directory = directory.getParentFile();
			if (directory != null)
				c.setCurrentDirectory(directory);
		} catch (Exception ignored) {
		}
		c.setFileFilter(new FileNameExtensionFilter("Warcraft III Campaign (*.w3n)", "w3n"));
		c.setAcceptAllFileFilterUsed(false);
		int rVal = c.showOpenDialog(SplitterFrame.this);
		if (rVal == JFileChooser.APPROVE_OPTION) {
			filePathField.setText(c.getSelectedFile().getAbsolutePath());
		}
		if (rVal == JFileChooser.CANCEL_OPTION) {
		}
	}

	public void openAction(ActionEvent e) {
		if (running)
			return;
		openAction(e, null);
	}

	public void clearAction(ActionEvent e) {
		if (running)
			return;
		filePathField.setText("");
	}

	public void splitAction(ActionEvent e) {
		if (running) {
			setButtonStates(false);
			if (splitterThread != null)
				splitterThread.interrupt();
			XT87Utils.deleteNewFiles();
			JOptionPane.showMessageDialog(null, "The splitting has stopped!", APP_TITLE, JOptionPane.INFORMATION_MESSAGE);
		} else {
			taskOutput.setText("");
			setButtonStates(true);
			splitterThread = new CampaignSplitterThread();
			splitterThread.start();
		}
	}

	public void setButtonStates(boolean running) {
		this.running = running;
		filePathField.setEditable(!running);
		Enumeration<AbstractButton> e = difficultySelectorGroup.getElements();
		for (Component c : componentList)
			c.setEnabled(!running);
		while (e.hasMoreElements()) {
			e.nextElement().setEnabled(!running);
		}
		if (running) {
			browse.setBackground(Color.GRAY);
			split.setText(getLocalizedString("stop_button"));
			split.setBackground(Color.RED);
		} else {
			browse.setBackground(new Color(100, 200, 255));
			split.setText(getLocalizedString("split_button"));
			split.setBackground(Color.ORANGE);
		}
	}

	private static String formatDuration(Duration duration) {
		long s = duration.getSeconds();
		return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
	}

	private class FrameCampaignSplitter extends CampaignSplitter {
		public FrameCampaignSplitter(File campFile) throws IOException {
			super(campFile);
		}

		@Override
		public void InitializeProgressBar(int i) {
			super.InitializeProgressBar(i);
			bar.setMaximum(i);
			bar.setValue(0);
		}

		@Override
		public void SetValueProgressBar(int i) {
			super.SetValueProgressBar(i);
			bar.setValue(i);
		}

		@Override
		public void IncrementValueProgressBar(int i) {
			super.IncrementValueProgressBar(i);
			if (initializedProgressBar)
				bar.setValue(bar.getValue() + i);
		}

		@Override
		public void CompleteProgressBar() {
			super.CompleteProgressBar();
			bar.setValue(bar.getMaximum());
		}
	}

	private static class AreaPrintStream extends PrintStream {
		private SplitterFrame frame;
		private String prefix;

		public AreaPrintStream(OutputStream out, SplitterFrame frame, String prefix) {
			super(out, true);
			this.frame = frame;
			this.prefix = prefix;
		}

		@Override
		public void println(String s) {
			super.println(s);
			if (!frame.taskOutput.getText().isEmpty())
				frame.taskOutput.append("\n");
			frame.taskOutput.append(prefix + s);
		}
	}

	private int getArchonModeAddedSlotsVariant() {
		return ((ArchonModeAddedSlotsVariant) Objects.requireNonNull(archonModeAddedSlotsDropdown.getSelectedItem())).getValue();
	}

	private void enableDisableToDownloadFolderCheck() {
		if (getArchonModeAddedSlotsVariant() > 0)
			toDownloadFolderCheck.setEnabled(!running);
		else {
			toDownloadFolderCheck.setEnabled(false);
			toDownloadFolderCheck.setSelected(false);
		}
	}

	class CampaignSplitterThread extends Thread {
		public final Instant startTime;

		CampaignSplitterThread() {
			this.startTime = Instant.now();
		}

		public void run() {
			File file = new File(filePathField.getText());
			try {
				CampaignSplitter cs = new FrameCampaignSplitter(file);
				cs.difficultySelectorOption = difficultySelectorOption;
				cs.withCampaignPreview = campaignPreviewCheck.isSelected();
				cs.withNextLevel = nextLevelCheck.isSelected();
				cs.withUpkeepRemoval = upkeepRemovalCheck.isSelected();
				cs.withLegacyAssets = legacyAssetsCheck.isSelected();
				cs.archonModeAddedSlots = getArchonModeAddedSlotsVariant();
				cs.toDownloadFolder = toDownloadFolderCheck.isSelected() && cs.archonModeAddedSlots > 0;
				cs.splitCampaign();
				String timeSpan = formatDuration(Duration.between(startTime, Instant.now()));
				JOptionPane.showMessageDialog(null, "Campaign \"" + file.getName() + "\" has been split successfully! (" + timeSpan + ")", APP_TITLE, JOptionPane.INFORMATION_MESSAGE);
			} catch (InterruptedException ignored) {
			} catch (Throwable ex) {
				try {
					if (file.exists()) {
						CampaignSplitter cs = new CampaignSplitter(file);
						cs.difficultySelectorOption = XT87Utils.TriOption.NO;
						cs.toDownloadFolder = toDownloadFolderCheck.isSelected() && getArchonModeAddedSlotsVariant() > 0;
						cs.removeTemporaryFiles();
					}
				} catch (IOException e) {
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					System.out.println("A problem was encountered:\n" + sw.toString().replaceAll("[\n\r](\0)*$", ""));
				}
				if (!isInterrupted()) {
					StringWriter sw = new StringWriter();
					ex.printStackTrace(new PrintWriter(sw));
					System.out.println("A problem was encountered:\n" + sw.toString().replaceAll("[\n\r](\0)*$", ""));
					String timeSpan = formatDuration(Duration.between(startTime, Instant.now()));
					String message = ex.getMessage();
					if (message == null)
						message = ex.toString();
					JOptionPane.showMessageDialog(null, "A problem was encountered when splitting \"" + file.getName() + "\"(" + timeSpan + "):\n" + message, APP_TITLE, JOptionPane.ERROR_MESSAGE);
				}
			} finally {
				setButtonStates(false);
			}
		}
	}

	private static class ActionListenerInPopup implements ActionListener {
		private final ActionListener l;

		public ActionListenerInPopup(ActionListener l) {
			this.l = l;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JPopupMenu popupMenu = (JPopupMenu) ((JMenuItem) e.getSource()).getParent(); // Get the parent menu
			popupMenu.setVisible(false);
			l.actionPerformed(e);
		}
	}

	public static void main(String[] args) {
		SplitterFrame frame = new SplitterFrame();
		PrintStream origOut = System.out;
		PrintStream areaOut = new AreaPrintStream(origOut, frame, "");
		System.setOut(areaOut);
		PrintStream origErr = System.err;
		PrintStream areaErr = new AreaPrintStream(origErr, frame, "ERR: ");
		System.setErr(areaErr);
	}
}