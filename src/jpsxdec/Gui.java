/*
 * jPSXdec: PlayStation 1 Media Decoder/Converter in Java
 * Copyright (C) 2007-2011  Michael Sabin
 * All rights reserved.
 *
 * Redistribution and use of the jPSXdec code or any derivative works are
 * permitted provided that the following conditions are met:
 *
 *  * Redistributions may not be sold, nor may they be used in commercial
 *    or revenue-generating business activities.
 *
 *  * Redistributions that are modified from the original source must
 *    include the complete source code, including the source code for all
 *    components used by a binary built from the modified sources. However, as
 *    a special exception, the source code distributed need not include
 *    anything that is normally distributed (in either source or binary form)
 *    with the major components (compiler, kernel, and so on) of the operating
 *    system on which the executable runs, unless that component itself
 *    accompanies the executable.
 *
 *  * Redistributions must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jpsxdec;

import com.l2fprod.common.swing.JDirectoryChooser;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import jpsxdec.GuiTree.TreeItem;
import jpsxdec.indexing.DiscIndex;
import jpsxdec.cdreaders.CdFileSectorReader;
import jpsxdec.discitems.DiscItemSaverBuilder;
import jpsxdec.discitems.DiscItemSaverBuilderGui;
import jpsxdec.discitems.IDiscItemSaver;
import jpsxdec.util.NotThisTypeException;
import jpsxdec.util.UserFriendlyHandler;
import jpsxdec.util.player.PlayController;
import jpsxdec.util.player.PlayController.Event;
import org.openide.awt.DropDownButtonFactory;

public class Gui extends javax.swing.JFrame {

    private static final Logger log = Logger.getLogger(Gui.class.getName());

    // -------------------------------------------------------------------------
    
    private DiscIndex _index;
    private PlayController _currentPlayer;
    private ArrayList<DiscItemSaverBuilderGui> _saverGuis =
            new ArrayList<DiscItemSaverBuilderGui>();
    private final GuiSettings _settings;

    // -------------------------------------------------------------------------

    public Gui() {

        // use the system's L&F if available (for great justice!)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            log.log(Level.WARNING, null, ex);
        }

        setTitle(Main.VerStringNonCommercial);

        initComponents();

        _guiTab.setEnabledAt(1, false);

        // center the gui
        this.setLocationRelativeTo(null);

        _settings = new GuiSettings();
        _settings.load();

        _guiDirectory.setText(_settings.getSavingDir());

        convertToolbar();

        try {
            setIconImage(ImageIO.read(Gui.class.getResource("jpsxdec16.png")));
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }


    // -------------------------------------------------------------------------
    // -- Toolbar dropdown tweak -----------------------------------------------
    // -------------------------------------------------------------------------

    private void convertToolbar() {
        _guiToolbar.removeAll();

        _guiOpenDisc = convertButton(_guiOpenDisc, new DiscMenu());
        _guiOpenIndex = convertButton(_guiOpenIndex, new IndexMenu());

        _guiToolbar.add(_guiOpenDisc);
        _guiToolbar.add(_guiToolbarSeparator1);
        _guiToolbar.add(_guiOpenIndex);
        _guiToolbar.add(_guiToolbarSeparator2);
        _guiToolbar.add(_guiSaveIndex);
        _guiToolbar.validate();
    }

    private static JButton convertButton(JButton oldButton, JPopupMenu menu) {
        JButton newButton = DropDownButtonFactory.createDropDownButton(oldButton.getIcon(), menu);
        newButton.setText(oldButton.getText());
        newButton.setHorizontalTextPosition(SwingConstants.LEFT);
        newButton.setMargin(oldButton.getMargin());
        for (ActionListener listener : oldButton.getActionListeners()) {
            newButton.addActionListener(listener);
        }
        return newButton;
    }

    private class DiscMenu extends JPopupMenu implements PopupMenuListener, ActionListener {

        private final JMenuItem EMPTY_MENU_ITEM = new JMenuItem("Empty");

        public DiscMenu() {
            add(EMPTY_MENU_ITEM);
            addPopupMenuListener(this);
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            removeAll();
            for (String sName : _settings.getPreviousImages()) {
                JMenuItem item = new JMenuItem(sName);
                item.addActionListener(this);
                add(item);
            }
        }

        public void actionPerformed(ActionEvent e) {
            openDisc(new File(e.getActionCommand()));
        }
        
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
        public void popupMenuCanceled(PopupMenuEvent e) {}
    }

    private class IndexMenu extends JPopupMenu implements PopupMenuListener, ActionListener {

        private final JMenuItem EMPTY_MENU_ITEM = new JMenuItem("Empty");
        
        public IndexMenu() {
            add(EMPTY_MENU_ITEM);
            addPopupMenuListener(this);
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            removeAll();
            for (String sName : _settings.getPreviousIndexes()) {
                JMenuItem item = new JMenuItem(sName);
                item.addActionListener(this);
                add(item);
            }
        }

        public void actionPerformed(ActionEvent e) {
            openIndex(new File(e.getActionCommand()));
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
        public void popupMenuCanceled(PopupMenuEvent e) {}
    }

    // -------------------------------------------------------------------------
    // -- Basic index load/save operations -------------------------------------
    // -------------------------------------------------------------------------


    private void openIndex(File indexFile) {
        File dir = indexFile.getParentFile();
        if (dir != null)
            _settings.setIndexDir(dir.getAbsolutePath());

        Logger log = Logger.getLogger("index");
        UserFriendlyHandler handler = new UserFriendlyHandler("index") {
            protected void onWarn(LogRecord record) {
                // TODO
            }
            protected void onErr(LogRecord record) {
                // TODO
            }
        };
        log.addHandler(handler);
        try {
            // TODO: prompt to save current index if not saved
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            setIndex(new DiscIndex(indexFile.getPath(), log), indexFile.getName());
            _settings.addPreviousIndex(indexFile.getAbsolutePath());
            _guiSaveIndex.setEnabled(false);
        } catch (NotThisTypeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error loading index file", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex);
        } finally {
            setCursor(Cursor.getDefaultCursor());
            log.removeHandler(handler);
            handler.close();
        }

    }

    private void setIndex(DiscIndex index, String sIndexFile) {
        _index = index;
        setDisc(_index.getSourceCD());
        _saverGuis.clear();
        
        _guiSaveAll.setEnabled(true);
        _guiSelectAll.setEnabled(true);

        _guiDiscTree.formatTreeTable(_index);

        if (sIndexFile == null) {
            sIndexFile = "*Unsaved*";
        }
        setIndexTitle(sIndexFile);

    }

    private void setIndexTitle(String sFile) {
        setTitle(sFile + " - " + Main.VerStringNonCommercial);
    }

    private File saveIndex() {
        if (_index == null)
            return null;

        BetterFileChooser fc = new BetterFileChooser(_settings.getIndexDir());
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setDialogTitle("Save index");
        fc.addChoosableFileFilter(GuiFileFilters.INDEX_FILE_FILTER);
        int iResult = fc.showSaveDialog(this);
        if (iResult != BetterFileChooser.APPROVE_OPTION)
            return null;
        _settings.setIndexDir(fc.getCurrentDirectory().getPath());
        try {

            File selection = fc.getSelectedFile();
            String sName = selection.getName();
            if (fc.getFileFilter().equals(GuiFileFilters.INDEX_FILE_FILTER) && !sName.toLowerCase().endsWith(".idx")) {
                File parent = selection.getParentFile();
                if (parent == null)
                    selection = new File(sName + ".idx");
                else
                    selection = new File(parent, sName + ".idx");
            }
            PrintStream ps = new PrintStream(selection);
            try {
                _index.serializeIndex(ps);
            } finally {
                ps.close();
            }

            return selection;
        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex);
            return null;
        }

    }

    // -------------------------------------------------------------------------
    // -- Basic disc load/save operations --------------------------------------
    // -------------------------------------------------------------------------

    private void setDisc(CdFileSectorReader cd) {
        _guiDiscInfoLine1.setText(cd.getSourceFile().getAbsoluteFile().getPath());
        _guiDiscInfoLine2.setText(cd.getTypeDescription());

        _settings.addPreviousImage(cd.getSourceFile().getAbsolutePath());
    }

    private void openDisc(File file) {
        File dir = file.getParentFile();
        if (dir != null)
            _settings.setImageDir(dir.getAbsolutePath());
        try {

            CdFileSectorReader cd = new CdFileSectorReader(file);

            if (!cd.hasSectorHeader())
                JOptionPane.showMessageDialog(this, "Disc image does not have raw headers -- audio may not be detected.");

            IndexingGui gui = new IndexingGui(this, cd);
            gui.setVisible(true);
            if (gui.getIndex() != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                setIndex(gui.getIndex(), null);
                _guiSaveIndex.setEnabled(true);
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        _guiTopPanel = new javax.swing.JPanel();
        _guiToolbar = new javax.swing.JToolBar();
        _guiOpenDisc = new javax.swing.JButton();
        _guiToolbarSeparator1 = new javax.swing.JToolBar.Separator();
        _guiOpenIndex = new javax.swing.JButton();
        _guiToolbarSeparator2 = new javax.swing.JToolBar.Separator();
        _guiSaveIndex = new javax.swing.JButton();
        _guiDiscInfoContainer = new javax.swing.JPanel();
        _guiDiscInfoLine1 = new javax.swing.JLabel();
        _guiDiscInfoLine2 = new javax.swing.JLabel();
        _guiMainSplit = new javax.swing.JSplitPane();
        _guiTreeContainer = new javax.swing.JPanel();
        _guiDiscTreeScroll = new javax.swing.JScrollPane();
        _guiDiscTree = new jpsxdec.GuiTree();
        _guiTreeButtonContainer = new javax.swing.JPanel();
        _guiSelectAll = new javax.swing.JButton();
        _guiSelectAllType = new javax.swing.JComboBox();
        _guiExpandAll = new javax.swing.JButton();
        _guiCollapseAll = new javax.swing.JButton();
        _guiTabContainer = new javax.swing.JPanel();
        _guiTab = new javax.swing.JTabbedPane();
        _guiSaveContainer = new javax.swing.JPanel();
        _guiDirChooserContainer = new javax.swing.JPanel();
        _guiDirectoryLbl = new javax.swing.JLabel();
        _guiDirectory = new javax.swing.JTextField();
        _guiChooseDir = new javax.swing.JButton();
        _guiSavePanel = new javax.swing.JPanel();
        _guiSaveBtnContainer = new javax.swing.JPanel();
        _guiApplyAll = new javax.swing.JButton();
        _guiSaveAll = new javax.swing.JButton();
        _guiPreviewContainer = new javax.swing.JPanel();
        _guiPlayPauseBtn = new javax.swing.JButton();
        _guiPreviewPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(new java.awt.BorderLayout(5, 5));

        _guiTopPanel.setLayout(new java.awt.BorderLayout());

        _guiToolbar.setFloatable(false);
        _guiToolbar.setRollover(true);

        _guiOpenDisc.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpsxdec/media-optical-5.png"))); // NOI18N
        _guiOpenDisc.setText("Analyzie Disc Image");
        _guiOpenDisc.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        _guiOpenDisc.setMargin(new java.awt.Insets(5, 10, 5, 10));
        _guiOpenDisc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiOpenDiscActionPerformed(evt);
            }
        });
        _guiToolbar.add(_guiOpenDisc);
        _guiToolbar.add(_guiToolbarSeparator1);

        _guiOpenIndex.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpsxdec/insert-numbers.png"))); // NOI18N
        _guiOpenIndex.setText("Open Index");
        _guiOpenIndex.setFocusable(false);
        _guiOpenIndex.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        _guiOpenIndex.setMargin(new java.awt.Insets(5, 10, 5, 10));
        _guiOpenIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiOpenIndexActionPerformed(evt);
            }
        });
        _guiToolbar.add(_guiOpenIndex);
        _guiToolbar.add(_guiToolbarSeparator2);

        _guiSaveIndex.setIcon(new javax.swing.ImageIcon(getClass().getResource("/jpsxdec/3floppy_unmount.png"))); // NOI18N
        _guiSaveIndex.setText("Save Index");
        _guiSaveIndex.setEnabled(false);
        _guiSaveIndex.setFocusable(false);
        _guiSaveIndex.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        _guiSaveIndex.setMargin(new java.awt.Insets(5, 10, 5, 10));
        _guiSaveIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiSaveIndexActionPerformed(evt);
            }
        });
        _guiToolbar.add(_guiSaveIndex);

        _guiTopPanel.add(_guiToolbar, java.awt.BorderLayout.NORTH);

        _guiDiscInfoContainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _guiDiscInfoContainer.setLayout(new javax.swing.BoxLayout(_guiDiscInfoContainer, javax.swing.BoxLayout.PAGE_AXIS));

        _guiDiscInfoLine1.setText("  ");
        _guiDiscInfoContainer.add(_guiDiscInfoLine1);

        _guiDiscInfoLine2.setText("  ");
        _guiDiscInfoContainer.add(_guiDiscInfoLine2);

        _guiTopPanel.add(_guiDiscInfoContainer, java.awt.BorderLayout.SOUTH);

        getContentPane().add(_guiTopPanel, java.awt.BorderLayout.NORTH);

        _guiMainSplit.setDividerLocation(500);
        _guiMainSplit.setResizeWeight(1.0);

        _guiTreeContainer.setLayout(new java.awt.BorderLayout(5, 5));

        _guiDiscTreeScroll.setPreferredSize(new java.awt.Dimension(500, 300));

        _guiDiscTree.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        _guiDiscTree.setMinimumSize(new java.awt.Dimension(200, 0));
        _guiDiscTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                _guiDiscTreeValueChanged(evt);
            }
        });
        _guiDiscTreeScroll.setViewportView(_guiDiscTree);

        _guiTreeContainer.add(_guiDiscTreeScroll, java.awt.BorderLayout.CENTER);

        _guiTreeButtonContainer.setLayout(new java.awt.GridBagLayout());

        _guiSelectAll.setText("Select ...");
        _guiSelectAll.setEnabled(false);
        _guiSelectAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiSelectAllActionPerformed(evt);
            }
        });
        _guiTreeButtonContainer.add(_guiSelectAll, new java.awt.GridBagConstraints());

        _guiSelectAllType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "none", "all Video", "all Audio (excluding video audio)", "all Audio (including video audio)", "all Files" }));
        _guiTreeButtonContainer.add(_guiSelectAllType, new java.awt.GridBagConstraints());

        _guiExpandAll.setText("Expand All");
        _guiExpandAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiExpandAllActionPerformed(evt);
            }
        });
        _guiTreeButtonContainer.add(_guiExpandAll, new java.awt.GridBagConstraints());

        _guiCollapseAll.setText("Collapse All");
        _guiCollapseAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiCollapseAllActionPerformed(evt);
            }
        });
        _guiTreeButtonContainer.add(_guiCollapseAll, new java.awt.GridBagConstraints());

        _guiTreeContainer.add(_guiTreeButtonContainer, java.awt.BorderLayout.SOUTH);

        _guiMainSplit.setLeftComponent(_guiTreeContainer);

        _guiTabContainer.setLayout(new java.awt.BorderLayout(5, 5));

        _guiTab.setMinimumSize(new java.awt.Dimension(400, 5));
        _guiTab.setPreferredSize(new java.awt.Dimension(400, 500));

        _guiSaveContainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        _guiSaveContainer.setMinimumSize(new java.awt.Dimension(0, 500));
        _guiSaveContainer.setPreferredSize(new java.awt.Dimension(0, 500));
        _guiSaveContainer.setLayout(new java.awt.BorderLayout());

        _guiDirChooserContainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));
        _guiDirChooserContainer.setLayout(new java.awt.BorderLayout(2, 0));

        _guiDirectoryLbl.setText("Directory:");
        _guiDirChooserContainer.add(_guiDirectoryLbl, java.awt.BorderLayout.WEST);
        _guiDirChooserContainer.add(_guiDirectory, java.awt.BorderLayout.CENTER);

        _guiChooseDir.setText("...");
        _guiChooseDir.setMargin(new java.awt.Insets(2, 3, 2, 2));
        _guiChooseDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiChooseDirActionPerformed(evt);
            }
        });
        _guiDirChooserContainer.add(_guiChooseDir, java.awt.BorderLayout.LINE_END);

        _guiSaveContainer.add(_guiDirChooserContainer, java.awt.BorderLayout.NORTH);

        _guiSavePanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        _guiSavePanel.setLayout(new java.awt.BorderLayout());
        _guiSaveContainer.add(_guiSavePanel, java.awt.BorderLayout.CENTER);

        _guiSaveBtnContainer.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        _guiApplyAll.setText("Apply to all ...");
        _guiApplyAll.setEnabled(false);
        _guiApplyAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiApplyAllActionPerformed(evt);
            }
        });
        _guiSaveBtnContainer.add(_guiApplyAll);

        _guiSaveAll.setText("Save All Selected");
        _guiSaveAll.setEnabled(false);
        _guiSaveAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiSaveAllActionPerformed(evt);
            }
        });
        _guiSaveBtnContainer.add(_guiSaveAll);

        _guiSaveContainer.add(_guiSaveBtnContainer, java.awt.BorderLayout.SOUTH);

        _guiTab.addTab("    Save    ", _guiSaveContainer);

        _guiPreviewContainer.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                _guiPreviewContainerComponentHidden(evt);
            }
            public void componentShown(java.awt.event.ComponentEvent evt) {
                _guiPreviewContainerComponentShown(evt);
            }
        });
        _guiPreviewContainer.setLayout(new java.awt.BorderLayout());

        _guiPlayPauseBtn.setText("Play");
        _guiPlayPauseBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                _guiPlayPauseBtnActionPerformed(evt);
            }
        });
        _guiPreviewContainer.add(_guiPlayPauseBtn, java.awt.BorderLayout.SOUTH);

        _guiPreviewPanel.setLayout(new java.awt.BorderLayout());
        _guiPreviewContainer.add(_guiPreviewPanel, java.awt.BorderLayout.CENTER);

        _guiTab.addTab("    Play    ", _guiPreviewContainer);

        _guiTabContainer.add(_guiTab, java.awt.BorderLayout.CENTER);
        _guiTab.getAccessibleContext().setAccessibleName("DoStuffTabs");

        _guiMainSplit.setRightComponent(_guiTabContainer);

        getContentPane().add(_guiMainSplit, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void _guiOpenIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiOpenIndexActionPerformed
        BetterFileChooser fc = new BetterFileChooser(_settings.getIndexDir());
        fc.setDialogTitle("Load index");
        fc.addChoosableFileFilter(GuiFileFilters.INDEX_FILE_FILTER);
        int iResult = fc.showOpenDialog(this);
        if (iResult != BetterFileChooser.APPROVE_OPTION)
            return;
        openIndex(fc.getSelectedFile());
        
    }//GEN-LAST:event__guiOpenIndexActionPerformed


    private void _guiDiscTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event__guiDiscTreeValueChanged
        TreeItem selection = _guiDiscTree.getTreeTblSelection();

        _guiTab.setSelectedIndex(0);
        JPanel gui = null;
        if (selection != null) {
            gui = getGuiForDiscItem(selection.getBuilder());
            _guiTab.setEnabledAt(1, selection.canPlay());
        }
        if (gui != null && !gui.isAncestorOf(_guiSavePanel)) {
            _guiSavePanel.removeAll();
            _guiSavePanel.add(gui);
            _guiApplyAll.setEnabled(true);
            _guiApplyAll.setText("Apply to all " + selection.getType());
        } else if (gui == null) {
            _guiSavePanel.removeAll();
            _guiApplyAll.setEnabled(false);
        }
        _guiSavePanel.revalidate();
        _guiSavePanel.repaint();

    }//GEN-LAST:event__guiDiscTreeValueChanged

    private DiscItemSaverBuilderGui getGuiForDiscItem(DiscItemSaverBuilder builder) {
        if (builder == null)
            return null;
        for (DiscItemSaverBuilderGui gui : _saverGuis) {
            if (gui.useSaverBuilder(builder)) {
                return gui;
            }
        }
        DiscItemSaverBuilderGui gui = builder.getOptionPane();
        if (gui != null)
            _saverGuis.add(gui);
        return gui;
    }



    private void _guiPreviewContainerComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event__guiPreviewContainerComponentShown
        TreeItem selection = _guiDiscTree.getTreeTblSelection();
        if (selection == null)
            return;
        _currentPlayer = selection.getPlayer();
        if (_currentPlayer != null) {
            if (_currentPlayer.hasVideo()) {
                _guiPreviewPanel.add(_currentPlayer.getVideoScreen());
                _guiPreviewPanel.revalidate();
            }
            _currentPlayer.addLineListener(_playerListener);
            _guiPlayPauseBtn.setText("Play");
            _guiPlayPauseBtn.setEnabled(true);
        }
    }//GEN-LAST:event__guiPreviewContainerComponentShown

    private final PlayController.PlayerListener _playerListener = new PlayController.PlayerListener() {
        public void update(Event eEvent) {
            switch (eEvent) {
                case Stop:
                    _guiPlayPauseBtn.setEnabled(false);
                    break;
            }
        }
    };


    private void _guiPreviewContainerComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event__guiPreviewContainerComponentHidden
        if (_currentPlayer != null) {
            try {
                _currentPlayer.stop();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            _currentPlayer = null;
            _guiPreviewPanel.removeAll();
            _guiPreviewPanel.validate();
            _guiPreviewPanel.repaint();
            System.gc();
        }
    }//GEN-LAST:event__guiPreviewContainerComponentHidden

    private void _guiPlayPauseBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiPlayPauseBtnActionPerformed
        try {
            if (_currentPlayer != null) {
                if (_guiPlayPauseBtn.getText().equals("Play")) {
                    _currentPlayer.play();
                    _guiPlayPauseBtn.setText("Pause");
                } else {
                    _currentPlayer.pause();
                    _guiPlayPauseBtn.setText("Play");
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }//GEN-LAST:event__guiPlayPauseBtnActionPerformed

    private void _guiOpenDiscActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiOpenDiscActionPerformed

        BetterFileChooser fc = new BetterFileChooser(_settings.getImageDir());
        fc.setDialogTitle("Select disc image or media file");
        for (FileFilter filter : GuiFileFilters.DISC_OPEN_FILTERS) {
            fc.addChoosableFileFilter(filter);
        }
        fc.setFileFilter(GuiFileFilters.DISC_OPEN_FILTERS[0]);
        int iResult = fc.showOpenDialog(this);
        if (iResult != BetterFileChooser.APPROVE_OPTION)
            return;

        openDisc(fc.getSelectedFile());

    }//GEN-LAST:event__guiOpenDiscActionPerformed



    private void _guiSaveIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiSaveIndexActionPerformed

        File file = saveIndex();
        if (file != null) {
            _guiSaveIndex.setEnabled(false);
            setIndexTitle(file.getName());
            _settings.addPreviousIndex(file.getAbsolutePath());
        }

    }//GEN-LAST:event__guiSaveIndexActionPerformed



    private void _guiSelectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiSelectAllActionPerformed
        _guiDiscTree.selectAllType(_guiSelectAllType.getSelectedItem().toString());
    }//GEN-LAST:event__guiSelectAllActionPerformed


    private void _guiExpandAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiExpandAllActionPerformed
        _guiDiscTree.expandAll();
    }//GEN-LAST:event__guiExpandAllActionPerformed

    private void _guiCollapseAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiCollapseAllActionPerformed
        _guiDiscTree.collapseAll();
    }//GEN-LAST:event__guiCollapseAllActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (_guiSaveIndex.isEnabled()) {
            int iRet = JOptionPane.showConfirmDialog(this, "The index has not been saved. Save index?", "Save index?", JOptionPane.YES_NO_CANCEL_OPTION);
            if (iRet == JOptionPane.CANCEL_OPTION)
                return;
            else if (iRet == JOptionPane.YES_OPTION) {
                if (saveIndex() == null)
                    return;
            }
        }
        if (_settings != null) {
            try {
            _settings.save();
            } catch (Throwable ex) {
                log.log(Level.WARNING, "Error saving ini file", ex);
            }
        }
        System.exit(0);
        
    }//GEN-LAST:event_formWindowClosing

    private void _guiApplyAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiApplyAllActionPerformed

        TreeItem node = _guiDiscTree.getTreeTblSelection();
        if (node == null)
            return;
        DiscItemSaverBuilder builder = node.getBuilder();
        if (builder != null) {
            int iCount = _guiDiscTree.applySettings(builder);
            JOptionPane.showMessageDialog(this, "Applied settings to " + iCount + " items.");
        }
    }//GEN-LAST:event__guiApplyAllActionPerformed


    private void _guiSaveAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiSaveAllActionPerformed

        try {

            ArrayList<IDiscItemSaver> savers = _guiDiscTree.collectSelected();
            if (savers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nothing is marked for saving.");
                return;
            }
            
            SavingGui gui = new SavingGui(
                    this, savers, new File(_guiDirectory.getText()));
            gui.setVisible(true);

        } catch (Throwable ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex);
        }

    }//GEN-LAST:event__guiSaveAllActionPerformed



    private void _guiChooseDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event__guiChooseDirActionPerformed

        JDirectoryChooser chooser = new JDirectoryChooser(_guiDirectory.getText());
        int choice = chooser.showOpenDialog(this);
        if(choice != JDirectoryChooser.APPROVE_OPTION)
            return;
        _guiDirectory.setText(chooser.getSelectedFile().getAbsolutePath());

    }//GEN-LAST:event__guiChooseDirActionPerformed



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton _guiApplyAll;
    private javax.swing.JButton _guiChooseDir;
    private javax.swing.JButton _guiCollapseAll;
    private javax.swing.JPanel _guiDirChooserContainer;
    private javax.swing.JTextField _guiDirectory;
    private javax.swing.JLabel _guiDirectoryLbl;
    private javax.swing.JPanel _guiDiscInfoContainer;
    private javax.swing.JLabel _guiDiscInfoLine1;
    private javax.swing.JLabel _guiDiscInfoLine2;
    private jpsxdec.GuiTree _guiDiscTree;
    private javax.swing.JScrollPane _guiDiscTreeScroll;
    private javax.swing.JButton _guiExpandAll;
    private javax.swing.JSplitPane _guiMainSplit;
    private javax.swing.JButton _guiOpenDisc;
    private javax.swing.JButton _guiOpenIndex;
    private javax.swing.JButton _guiPlayPauseBtn;
    private javax.swing.JPanel _guiPreviewContainer;
    private javax.swing.JPanel _guiPreviewPanel;
    private javax.swing.JButton _guiSaveAll;
    private javax.swing.JPanel _guiSaveBtnContainer;
    private javax.swing.JPanel _guiSaveContainer;
    private javax.swing.JButton _guiSaveIndex;
    private javax.swing.JPanel _guiSavePanel;
    private javax.swing.JButton _guiSelectAll;
    private javax.swing.JComboBox _guiSelectAllType;
    private javax.swing.JTabbedPane _guiTab;
    private javax.swing.JPanel _guiTabContainer;
    private javax.swing.JToolBar _guiToolbar;
    private javax.swing.JToolBar.Separator _guiToolbarSeparator1;
    private javax.swing.JToolBar.Separator _guiToolbarSeparator2;
    private javax.swing.JPanel _guiTopPanel;
    private javax.swing.JPanel _guiTreeButtonContainer;
    private javax.swing.JPanel _guiTreeContainer;
    // End of variables declaration//GEN-END:variables


    public static void main(String args[]) {
        Main.loadDefaultLogger();
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Gui().setVisible(true);
            }
        });
    }

}