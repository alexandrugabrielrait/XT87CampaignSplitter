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
	public static final String APP_TITLE = AUTHOR + "'s " + "Campaign Splitter";
	private final JTextField filePathField = new JTextField(30);
	private final JButton openButton = new JButton(getLocalizedString("button_open"));
	private final JButton splitButton = new JButton(getLocalizedString("button_split"));
	private boolean running = false, restart = false;
	private Thread splitterThread = null;
	private final JProgressBar bar;
	private final JTextArea taskOutput;
	private XT87Utils.TriOption difficultySelectorOption = XT87Utils.TriOption.SMART;
	private final ButtonGroup difficultySelectorGroup;
	private final JCheckBox campaignPreviewCheck = new JCheckBox();
	private final JCheckBox nextLevelCheck = new JCheckBox();
	private final JCheckBox legacyAssetsSDCheck = new JCheckBox();
	private final JCheckBox legacyAssetsHDCheck = new JCheckBox();
	private final JCheckBox upkeepRemovalCheck = new JCheckBox();
	private final JCheckBox toDownloadFolderCheck = new JCheckBox();
	private final JComboBox<ArchonModeAddedSlotsVariant> archonModeAddedSlotsDropdown = new JComboBox<>();
	private final List<? extends JComponent> componentList = Arrays.asList(openButton, campaignPreviewCheck, nextLevelCheck, legacyAssetsSDCheck, legacyAssetsHDCheck, upkeepRemovalCheck, toDownloadFolderCheck);

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

	private static class LanguageVariant {
		private final SplitterLanguage language;

		private LanguageVariant(SplitterLanguage language) {
			this.language = language;
		}

		public SplitterLanguage getValue() {
			return language;
		}

		public String toString() {
			return language.getDisplayName();
		}
	}

	private static JPanel createPanel(Container parent) {
		JPanel panel = new JPanel();
		parent.add(panel);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		return panel;
	}

	private static void addTooltips(String labelTag, JLabel label, JComponent component) {
		String tooltipText = getLocalizedString(labelTag + "_tooltip");
		label.setToolTipText(tooltipText);
		component.setToolTipText(tooltipText);
	}

	private static JLabel addLabelAndComponent(JPanel panel, String labelTag, JComponent component) {
		JLabel label = new JLabel(getLocalizedString(labelTag));
		panel.add(label);
		panel.add(component);
		addTooltips(labelTag, label, component);
		return label;
	}

	public SplitterFrame() {
		super(APP_TITLE);
		setSize(700, 700);
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLayout(new FlowLayout());

		JPanel optionsPanel = new JPanel(new GridLayout(0, 1));
		add(optionsPanel);

		JPanel panel = new JPanel();
		optionsPanel.add(panel);
		JLabel label = new JLabel(APP_TITLE + " v" + CampaignSplitter.getVersion());
		panel.add(label);
		label.setFont(label.getFont().deriveFont(label.getFont().getStyle() | Font.BOLD));

		panel = new JPanel();
		optionsPanel.add(panel);
		String labelTag = "difficulty_selector_check";
		label = new JLabel(getLocalizedString(labelTag));
		panel.add(label);
		label.setToolTipText(getLocalizedString(labelTag + "_tooltip"));
		difficultySelectorGroup = new ButtonGroup();
		JRadioButton radioButton;
		for (XT87Utils.TriOption option : XT87Utils.TriOption.values()) {
			labelTag = "difficulty_selector_" + option.toString().toLowerCase(Locale.ROOT);
			radioButton = new JRadioButton(getLocalizedString(labelTag));
			radioButton.setFocusPainted(false);
			radioButton.setActionCommand(option.toString());
			if (option == difficultySelectorOption)
				radioButton.setSelected(true);
			radioButton.setToolTipText(getLocalizedString(labelTag + "_tooltip"));
			radioButton.addActionListener(new DifficultySelectorOptionL(option));
			panel.add(radioButton);
			difficultySelectorGroup.add(radioButton);
		}

		panel = createPanel(optionsPanel);
		addLabelAndComponent(panel, "campaign_preview_check", campaignPreviewCheck);
		campaignPreviewCheck.setSelected(true);
		addLabelAndComponent(panel, "next_level_name_check", nextLevelCheck);
		nextLevelCheck.setSelected(true);

		panel = createPanel(optionsPanel);
		addLabelAndComponent(panel, "legacy_assets_sd_check", legacyAssetsSDCheck);
		legacyAssetsSDCheck.setSelected(true);
		addLabelAndComponent(panel, "legacy_assets_hd_check", legacyAssetsHDCheck);
		legacyAssetsHDCheck.setSelected(false);
		addLabelAndComponent(panel, "remove_upkeep_check", upkeepRemovalCheck);
		upkeepRemovalCheck.setSelected(false);

		panel = createPanel(optionsPanel);
		addLabelAndComponent(panel, "archon_mode_dropdown", archonModeAddedSlotsDropdown);
		for (int i = 0; i < 24; i++)
			archonModeAddedSlotsDropdown.addItem(new ArchonModeAddedSlotsVariant(i));
		archonModeAddedSlotsDropdown.addItemListener(e -> {
			enableDisableToDownloadFolderCheck();
		});
		JLabel toDownloadFolderLabel = addLabelAndComponent(panel, "to_download_folder_check", toDownloadFolderCheck);
		enableDisableToDownloadFolderCheck();

		openButton.addActionListener(this::openAction);
		splitButton.addActionListener(this::splitAction);
		panel = createPanel(this);
		panel.add(filePathField);
		panel.add(openButton);
		openButton.setFocusPainted(false);
		openButton.setFocusable(false);
		openButton.setBackground(new Color(100, 200, 255));
		panel.add(splitButton);
		splitButton.setFocusPainted(false);
		splitButton.setFocusable(false);
		splitButton.setBackground(Color.ORANGE);

		panel = new JPanel();
		add(panel);
		bar = new JProgressBar();
		bar.setValue(0);
		bar.setMaximum(100);
		bar.setStringPainted(true);
		panel.add(bar);

		panel = new JPanel();
		add(panel);
		taskOutput = new JTextArea(20, 80);
		taskOutput.setMargin(new Insets(5, 5, 5, 5));
		taskOutput.setEditable(false);
		panel.add(new JScrollPane(taskOutput), BorderLayout.CENTER);

		bar.setPreferredSize(new Dimension((int) (0.95f * getWidth()), bar.getPreferredSize().height));

		try {
			setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getResource("/Icon.png"))).getImage());
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JMenu menu;
		JMenuItem menuItem;
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		menu = new JMenu(getLocalizedString("menu_tab_file"));
		menuBar.add(menu);

		menuItem = new JMenuItem(getLocalizedString("menu_item_open"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('O', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(this::openAction));
		menu.add(menuItem);

		menuItem = new JMenuItem(getLocalizedString("menu_item_open_maps"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('M', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e -> openAction(e, XT87Utils.MAPS_FOLDER)));
		menu.add(menuItem);

		menuItem = new JMenuItem(getLocalizedString("menu_item_open_campaigns"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('N', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e -> openAction(e, XT87Utils.CAMPAIGNS_FOLDER)));
		menu.add(menuItem);

		menuItem = new JMenuItem(getLocalizedString("menu_item_clear"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('W', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(this::clearAction));
		menu.add(menuItem);

		menuItem = new JMenuItem(getLocalizedString("menu_item_preferences"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('F', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e ->
		{
			if (running)
				return;
			JComboBox<LanguageVariant> languageDropdown = new JComboBox<>();
			Arrays.stream(SplitterLanguage.values()).forEach(x ->
			{
				LanguageVariant variant = new LanguageVariant(x);
				languageDropdown.addItem(variant);
				if (x == SplitterLanguage.selectedLanguage)
					languageDropdown.setSelectedItem(variant);
			});

			JPanel panel2 = new JPanel();
			panel2.add(new JLabel("Select Language:"));
			panel2.add(languageDropdown);

			int result = JOptionPane.showConfirmDialog(null, panel2, "Preferences",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

			if (result == JOptionPane.OK_OPTION) {
				SplitterLanguage selectedLanguage = ((LanguageVariant) Objects.requireNonNull(languageDropdown.getSelectedItem())).language;
				SplitterLanguage.selectedLanguage = selectedLanguage;
				JOptionPane.showMessageDialog(null, String.format(getLocalizedString("language_selected_message"), selectedLanguage.getDisplayName()),
						"Preferences Saved", JOptionPane.INFORMATION_MESSAGE);
				SplitterLanguage.saveLanguage(selectedLanguage);
				dispose();
				startFrame();
			}
		}
		));
		menu.add(menuItem);

		menuItem = new JMenuItem(getLocalizedString("menu_item_exit"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));
		menuItem.addActionListener(e -> System.exit(0));
		menu.add(menuItem);

		menu = new JMenu(getLocalizedString("menu_tab_edit"));
		menuBar.add(menu);

		menuItem = new JMenuItem(getLocalizedString("menu_item_split_stop"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('S', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(this::splitAction));
		menu.add(menuItem);

		menuItem = new JMenuItem(getLocalizedString("menu_item_uncheck_all"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('J', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(e -> {
			if (running)
				return;
			campaignPreviewCheck.setSelected(false);
			nextLevelCheck.setSelected(false);
			legacyAssetsSDCheck.setSelected(false);
			legacyAssetsHDCheck.setSelected(false);
			upkeepRemovalCheck.setSelected(false);
			archonModeAddedSlotsDropdown.setSelectedIndex(0);
			toDownloadFolderCheck.setSelected(false);
		});
		menu.add(menuItem);

		menu = new JMenu(getLocalizedString("menu_tab_about"));
		menuBar.add(menu);

		menuItem = new JMenuItem(getLocalizedString("menu_item_help"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('H', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e ->
				JOptionPane.showMessageDialog(null,
						"Click \"" + openButton.getText() + "\" to select a Warcraft III Custom Campaign file (\"*.w3n\").\n" +
								"Click \"" + splitButton.getText() + "\" to split the campaign (the campaign file will not be altered).\n" +
								"A folder with the same name as the campaign will be created in the same location.\n" +
								"This folder will contain all extracted maps, merged with campaign data.\n" +
								"\n" +
								"For Archon Mode, all players need to select the exact same options, use the same version of the tool and of the campaign!\n" +
								"The lobby host needs to have the \"" + toDownloadFolderLabel.getText() + "\" option unchecked.\n" +
								"All other players need to have the \"" + toDownloadFolderLabel.getText() + "\" option checked.",
						APP_TITLE + " - " + getLocalizedString("menu_item_help"), JOptionPane.QUESTION_MESSAGE))
		);
		menu.add(menuItem);

		menuItem = new JMenuItem(getLocalizedString("menu_item_tutorial"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('T', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e -> browseAction(e, "https://www.youtube.com/watch?v=n902PNEm2DM")));
		menu.add(menuItem);

		menuItem = new JMenuItem(getLocalizedString("menu_item_web_page"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('G', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e -> browseAction(e, "https://www.hiveworkshop.com/threads/xetanth87s-campaign-splitter-play-custom-campaigns-in-reforged.340521/")));
		menu.add(menuItem);

		menu = new JMenu(getLocalizedString("menu_tab_donate"));
		menuBar.add(menu);

		menuItem = new JMenuItem(getLocalizedString("menu_item_donate_patron"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('P', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e -> browseAction(e, "https://www.patreon.com/alternatecampaigns")));
		menu.add(menuItem);

		menuItem = new JMenuItem(getLocalizedString("menu_item_donate_once"));
		menuItem.setAccelerator(KeyStroke.getKeyStroke('K', KeyEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(new ActionListenerInPopup(e -> browseAction(e, "https://www.buymeacoffee.com/xetanth87")));
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
			String absolutePath = c.getSelectedFile().getAbsolutePath();
			if (!absolutePath.equals(filePathField.getText())) {
				filePathField.setText(absolutePath);
				taskOutput.setText("");
				bar.setValue(0);
			}
		}
		if (rVal == JFileChooser.CANCEL_OPTION) {
		}
	}

	public void openAction(ActionEvent e) {
		if (running)
			return;
		openAction(e, null);
	}

	public void browseAction(ActionEvent e, String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (IOException | URISyntaxException ex) {
			ex.printStackTrace();
		}
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
			JOptionPane.showMessageDialog(null, "The splitting has stopped!", APP_TITLE + " - " + getLocalizedString("button_stop"), JOptionPane.WARNING_MESSAGE);
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
			openButton.setBackground(Color.GRAY);
			splitButton.setText(getLocalizedString("button_stop"));
			splitButton.setBackground(Color.RED);
		} else {
			openButton.setBackground(new Color(100, 200, 255));
			splitButton.setText(getLocalizedString("button_split"));
			splitButton.setBackground(Color.ORANGE);
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
				cs.withLegacyAssetsSD = legacyAssetsSDCheck.isSelected();
				cs.withLegacyAssetsHD = legacyAssetsHDCheck.isSelected();
				cs.archonModeAddedSlots = getArchonModeAddedSlotsVariant();
				cs.toDownloadFolder = toDownloadFolderCheck.isSelected() && cs.archonModeAddedSlots > 0;
				cs.splitCampaign();
				String timeSpan = formatDuration(Duration.between(startTime, Instant.now()));
				JOptionPane.showMessageDialog(null, "Campaign \"" + file.getName() + "\" has been split successfully! (" + timeSpan + ")", APP_TITLE + " - " + getLocalizedString("title_success"), JOptionPane.INFORMATION_MESSAGE);
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
					JOptionPane.showMessageDialog(null, "A problem was encountered when splitting \"" + file.getName() + "\"(" + timeSpan + "):\n" + message, APP_TITLE + " - " + getLocalizedString("title_error"), JOptionPane.ERROR_MESSAGE);
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

	private static void startFrame() {
		SplitterLanguage.loadLanguage();
		SplitterFrame frame = new SplitterFrame();
		PrintStream origOut = System.out;
		PrintStream areaOut = new AreaPrintStream(origOut, frame, "");
		System.setOut(areaOut);
		PrintStream origErr = System.err;
		PrintStream areaErr = new AreaPrintStream(origErr, frame, "ERR: ");
		System.setErr(areaErr);
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		startFrame();
	}
}