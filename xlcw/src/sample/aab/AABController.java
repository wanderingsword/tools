package sample.aab;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


//adb logcat -c  清除日志缓存
//adb logcat > log.txt  拉取 Android 日志，保存到当前目录的 log.txt 中
//del log.txt 删除当前目录下的 log.txt 文件
//keytool -printcert -file CERT.RSA 查看证书指纹
public class AABController {
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    @FXML
    private GridPane root;

    @FXML
    private TextArea showInfo;

    @FXML
    private TextField aab_path;
    @FXML
    private TextField apks_path;

    @FXML
    private TextField ks_path;
    @FXML
    private TextField ks_alias;
    @FXML
    private TextField ks_alias_pass;
    @FXML
    private TextField ks_pass;
    @FXML
    private ComboBox<String> devices_id;

    ProcessBuilder builder = new ProcessBuilder();
    StringBuilder showText = new StringBuilder();
    List<String> convertApksArgs = null;
    List<String> installApksArgs = null;

    @FXML
    protected void convertApks(ActionEvent event) {
        String aabPath = aab_path.getText();
        showText.delete(0, showText.length());

        if (isEmpty(aabPath) || !aabPath.endsWith("aab")) {
            error("请选择 aab 文件");
            return;
        }

        if (isEmpty(apks_path.getText())) {
            error("apks 输出路径不能为空");
            return;
        }

        if (isEmpty(ks_path.getText())) {
            error("签名文件不能为空");
            return;
        }

        if (isEmpty(ks_pass.getText()) || isEmpty(ks_alias.getText()) || isEmpty(ks_alias_pass.getText())) {
            error("请填入正确的签名信息");
            return;
        }

        showText.delete(0, showText.length());
        showText.append("converting aab to apks with device ").append(devices_id.getValue()).append("...\n");
        showText.append("run command: ").append("java -jar ").append(getJarPath()).append(File.separator).append("bundletool.jar build-apks --connected-device --local-testing");
        if (!isEmpty(devices_id.getValue())) {
            showText.append(" --device-id=").append(devices_id.getValue());
        }
        showText.append(" --bundle=").append(aabPath).append(" --output=").append(apks_path.getText()).append(" --ks=").append(ks_path.getText()).append(" --ks-pass=pass:").append(ks_pass.getText()).append(" --ks-key-alias=").append(ks_alias.getText()).append(" --key-pass=pass:").append(ks_alias_pass.getText()).append("\n");
        showInfo.setText(showText.toString());

        executorService.execute(convertApks);
        showText.append("正在将 aab 转成 apks，请等待.....\n");
        showInfo.setText(showText.toString());
    }

    @FXML
    protected void installApks(ActionEvent event) {
        if (isEmpty(apks_path.getText())) {
            error("请选择正确的 apks 文件");
            return;
        }
        showText.append("run commmand: java -jar ").append(getJarPath()).append(File.separator).append("bundletool.jar install-apks --apks=").append(apks_path.getText());
        if (!isEmpty(devices_id.getValue())) {
            showText.append(" --device-id=").append(devices_id.getValue());
        }
        showText.append("\n");
        showInfo.setText(showText.toString());
        executorService.execute(installApks);
        showText.append("正在安装 apks，请等待.....\n");
        showInfo.setText(showText.toString());
    }


    ArrayList<String> devices = new ArrayList<>();

    @FXML
    private void getDeviceInfo(ActionEvent event) {
        BufferedReader reader = null;
        showInfo.setText("checking devices");
        builder.command("adb.exe", "devices");
        try {
            Process process = builder.start();
            process.waitFor();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s;
            while ((s = reader.readLine()) != null) {
                if (s.contains("device") && s.contains("\t")) {
                    String device = s.substring(0, s.indexOf("\t"));
                    if (!devices.contains(device)) {
                        devices.add(device);
                    }
                }
                System.out.println(s);
                showText.append(s).append("\n");
                showInfo.setText(showText.toString());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        devices_id.getItems().addAll(devices);
        devices_id.setValue(devices.get(0));
        showInfo.setText("设备检索完成");
    }


    public void onDragOver(DragEvent dragEvent) {
        if (dragEvent.getGestureSource() != root && dragEvent.getDragboard().hasFiles()) {
            dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        dragEvent.consume();
    }

    public void onDragDropped(DragEvent dragEvent) {
        Dragboard dragboard = dragEvent.getDragboard();
        if (dragboard.hasFiles()) {
            File dragFile = dragboard.getFiles().get(0);
            String filePath = dragFile.getPath();
            if (filePath.endsWith("aab")) {
                aab_path.setText(filePath);
                apks_path.setText(filePath.replace(".aab", ".apks"));
            } else if (dragFile.getName().endsWith("jks")) {
                ks_path.setText(filePath);
            } else if (dragFile.getName().endsWith(".apks")) {
                apks_path.setText(dragFile.getPath());
            }
        }
    }

    public void openPath(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        if (event.getTarget() == apks_path) {
            chooser.setInitialDirectory(new File(apks_path.getText()));
            File file = chooser.showDialog(root.getScene().getWindow());
            apks_path.setText(file.getAbsolutePath());
        } else if (event.getTarget() == ks_path) {
            chooser.setInitialDirectory(new File(ks_path.getText()));
            File file = chooser.showDialog(root.getScene().getWindow());
            ks_path.setText(file.getPath());
        }
    }

    private boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    private void error(String s) {
        showInfo.setText(s);
        showText.delete(0, showText.length());
    }

    private final Runnable convertApks = new Runnable() {
        @Override
        public void run() {
            BufferedReader reader = null;
            BufferedReader errorReader = null;

            if (convertApksArgs == null || convertApksArgs.size() == 0) {
                convertApksArgs = buildConvertArgs();
            }
            builder.command(convertApksArgs);
            try {
                Process process = builder.start();
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String info;
                String errorString;
                while ((info = reader.readLine()) != null) {
                    showText.append(info).append("\n");
                    showInfo.setText(showText.toString());
                }

                while ((errorString = errorReader.readLine()) != null) {
                    showText.append(errorString).append("\n");
                    showInfo.setText(showText.toString());
                }

                showText.append("convert success\n");
                showInfo.setText(showText.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null)
                        reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private final Runnable installApks = new Runnable() {
        @Override
        public void run() {
            if (installApksArgs == null || installApksArgs.size() == 0) {
                installApksArgs = buildInstallApksArgs();
            }
            builder.command(installApksArgs);
            try {
                Process process = builder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                process.waitFor();

                String info;
                while ((info = reader.readLine()) != null) {
                    showText.append(info).append("\n");
                    showInfo.setText(showText.toString());
                }

                String error;
                while ((error = errorReader.readLine()) != null) {
                    showText.append(error).append("\n");
                    showInfo.setText(showText.toString());
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            showText.append("install complete\n");
            showInfo.setText(showText.toString());
        }
    };

    private List<String> buildConvertArgs() {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-jar");
        commands.add("tools" + File.separator + "bundletool.jar");
        commands.add("build-apks");
        commands.add("--local-testing");
        commands.add("--connected-device");
        if (!isEmpty(devices_id.getValue())) {
            commands.add("--device-id");
            commands.add(devices_id.getValue());
        }
        commands.add("--bundle");
        commands.add(aab_path.getText());
        commands.add("--output");
        commands.add(apks_path.getText());
        commands.add("--ks");
        commands.add(ks_path.getText());
        commands.add("--ks-pass");
        commands.add("pass:" + ks_pass.getText());
        commands.add("--ks-key-alias");
        commands.add(ks_alias.getText());
        commands.add("--key-pass");
        commands.add("pass:" + ks_alias_pass.getText());
        return commands;
    }

    private List<String> buildInstallApksArgs() {
        ArrayList<String> args = new ArrayList<>();
        builder.command("java", "-jar", "tools" + File.separator + "bundletool.jar", "install-apks", "--apks", apks_path.getText(), "--device-id", devices_id.getValue());
        args.add("java");
        args.add("-jar");
        args.add("tools" + File.separator + "bundletool.jar");
        args.add("install-apks");
        args.add("--apks");
        args.add(apks_path.getText());
        if (!isEmpty(devices_id.getValue())) {
            args.add("--device-id");
            args.add(devices_id.getValue());
        }
        return args;
    }

    private String getJarPath() {

        //builder.directory().getPath()
        File file = new File("tools");
        if (file.exists()) {
            return file.getPath();
        }
        return "tools";
    }
}
