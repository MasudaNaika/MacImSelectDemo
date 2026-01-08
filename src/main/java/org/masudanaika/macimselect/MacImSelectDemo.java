package org.masudanaika.macimselect;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/**
 * MacImSelectDemo.
 *
 * @author masuda, Masudana Ika
 */
public class MacImSelectDemo {

    public static void main(String... args) {
        MacImSelectDemo test = new MacImSelectDemo();
        test.start();
    }

    private void start() {

        MacImSelectJNA imSelectJNA = new MacImSelectJNA();
        MacImSelectFFM imSelectFFM = new MacImSelectFFM();

        JFrame frame = new JFrame("MacImSelect Demo, (C)Masudana Ika");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        frame.setContentPane(panel);

        JTextArea ta = new JTextArea(5, 38);
        panel.add(ta, BorderLayout.CENTER);
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JToggleButton tgl = new JToggleButton("JNA");
        tgl.addActionListener(ae -> {
            if (tgl.isSelected()) {
                tgl.setText("FFM");
            } else {
                tgl.setText("JNA");
            }
        });
        JButton btn1 = new JButton("英数字");
        btnPanel.add(btn1);
        btn1.addActionListener(ae -> {
            if (tgl.isSelected()) {
                imSelectFFM.toRomanMode();
            } else {
                imSelectJNA.toRomanMode();
            }
            ta.setText("Roman mode.\n");
        });
        JButton btn2 = new JButton("漢字");
        btnPanel.add(btn2);
        btn2.addActionListener(ae -> {
            if (tgl.isSelected()) {
                imSelectFFM.toKanjiMode();
            } else {
                imSelectJNA.toKanjiMode();
            }
            ta.setText("Kanji mode.\n");
        });
        JButton btn3 = new JButton("現在");
        btnPanel.add(btn3);
        btn3.addActionListener(ae -> {
            String sourceId;
            if (tgl.isSelected()) {
                sourceId = imSelectFFM.getSelectedInputSourceId();
            } else {
                sourceId = imSelectJNA.getSelectedInputSourceId();
            }
            ta.setText(sourceId + "\n");
        });
        JButton btn4 = new JButton("リスト");
        btnPanel.add(btn4);
        btn4.addActionListener(ae -> {
            List<String> list;
            if (tgl.isSelected()) {
                list = imSelectFFM.getInputSourceList();
            } else {
                list = imSelectJNA.getInputSourceList();
            }
            String str = list.stream().collect(Collectors.joining("\n"));
            ta.setText(str);
        });
        btnPanel.add(tgl);
        panel.add(btnPanel, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);

        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
        });
    }

}
