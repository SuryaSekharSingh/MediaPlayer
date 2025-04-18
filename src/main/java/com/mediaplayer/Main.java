package com.mediaplayer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.util.Objects;

import javax.swing.JLabel;

public class Main extends Application {
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Label currentTimeLabel, totalTimeLabel;
    private Slider progressSlider, volumeSlider;
    private Button playPauseButton;
    private HBox topBar, controlBar;
    boolean isMuted = false;
    private double savedVolume;
    private Timeline hideControlsTimeline;
    private boolean isFullscreen = false;

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        BorderPane mainPane = new BorderPane();

        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);

        topBar = createTopBar(primaryStage);
        controlBar = createControlBar();

        mainPane.setTop(topBar);
        mainPane.setBottom(controlBar);

        root.getChildren().addAll(mediaView, mainPane);

        Scene scene = new Scene(root, 1000, 700);
        String css = getClass().getResource("/styles.css") != null
                ? Objects.requireNonNull(getClass().getResource("/styles.css")).toExternalForm()
                : null;
        if (css != null)
            scene.getStylesheets().add(css);

        setupFullscreenBehavior(scene, primaryStage);
        setupMediaViewSizeBinding(scene);
        setupEnterKeyControl(scene);
        primaryStage.setTitle("RythmX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Add missing helper methods
    private Button createIconButton(String icon) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-font-size: 16; -fx-background-color: transparent; -fx-text-fill: white;");
        return btn;
    }

    private String formatTime(Duration duration) {
        int minutes = (int) duration.toMinutes();
        int seconds = (int) duration.toSeconds() % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void setupEnterKeyControl(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (Objects.requireNonNull(event.getCode()) == KeyCode.ENTER) {
                togglePlayPause();
            }
        });
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playPauseButton.setText("▶");
            } else {
                mediaPlayer.play();
                playPauseButton.setText("⏸");
            }
        }
    }

    private void setupFullscreenBehavior(Scene scene, Stage stage) {
        stage.fullScreenProperty().addListener((obs, oldVal, newVal) -> {
            isFullscreen = newVal;
            if (newVal) {
                topBar.setVisible(false);
                controlBar.setVisible(false);
                if (mediaPlayer != null)
                    mediaPlayer.play();
            } else {
                topBar.setVisible(true);
                controlBar.setVisible(true);
            }
        });
        scene.setOnMouseMoved(e -> {
            if (isFullscreen) {
                showTemporaryControls();
            }
        });
    }

    private void showTemporaryControls() {
        controlBar.setVisible(true);
        if (hideControlsTimeline != null)
            hideControlsTimeline.stop();

        hideControlsTimeline = new Timeline(new KeyFrame(
                Duration.seconds(5),
                ae -> controlBar.setVisible(false)));
        hideControlsTimeline.play();
    }

    private void setupMediaViewSizeBinding(Scene scene) {
        mediaView.fitWidthProperty().bind(scene.widthProperty());
        mediaView.fitHeightProperty().bind(scene.heightProperty());
    }

    private HBox createTopBar(Stage stage) {
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: #2b2b2b;");

        Button openButton = createStyledButton("File");
        Button fullscreenButton = createStyledButton("Fullscreen");
        Button muteButton = createStyledButton("Mute");

        openButton.setOnAction(e -> openMediaFile(stage));
        fullscreenButton.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        muteButton.setOnAction(e -> toggleMute());

        topBar.getChildren().addAll(openButton, fullscreenButton, muteButton);
        return topBar;
    }

    private HBox createControlBar() {

        HBox timeButtons = new HBox(10);
        timeButtons.setPadding(new Insets(0));
        timeButtons.setAlignment(Pos.CENTER);
        timeButtons.setStyle("-fx-background-color: rgba(60, 60, 60, 0.7);");

        HBox progressBox = new HBox(6);
        progressBox.setPadding(new Insets(15));
        progressBox.setAlignment(Pos.CENTER);
        progressBox.setStyle("-fx-background-color: rgba(60, 60, 60, 0.7);");

        VBox timeBox = new VBox(6);
        timeBox.setPadding(new Insets(0));
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setStyle("-fx-background-color: rgba(60, 60, 60, 0.7);");

        VBox volumeBox = new VBox(10);
        timeBox.setPadding(new Insets(0));
        timeBox.setAlignment(Pos.TOP_CENTER);
        timeBox.setStyle("-fx-background-color: rgba(60, 60, 60, 0.7);");

        VBox speedBox = new VBox(5);
        timeBox.setPadding(new Insets(0));
        timeBox.setAlignment(Pos.TOP_CENTER);
        timeBox.setStyle("-fx-background-color: rgba(60, 60, 60, 0.7);");

        HBox controlBar = new HBox(15);
        controlBar.setPadding(new Insets(15));
        controlBar.setAlignment(Pos.CENTER);
        controlBar.setStyle("-fx-background-color: rgba(60, 60, 60, 0.7);");

        playPauseButton = createIconButton("▶");
        Button prevButton = createIconButton("⏮");
        Button nextButton = createIconButton("⏭");
        Button rewindButton = createIconButton("⏪");
        Button forwardButton = createIconButton("⏩");

        progressSlider = new Slider();
        progressSlider.setPrefWidth(400);

        progressSlider.getStyleClass().add("custom-slider");

        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double percent = (newVal.doubleValue() - progressSlider.getMin()) /
                    (progressSlider.getMax() - progressSlider.getMin()) * 100;

            progressSlider.lookup(".track").setStyle(
                    "-fx-background-color: linear-gradient(to right,rgb(238, 138, 75) " + percent + "%, #ddd " + percent
                            + "%);");
        });

        currentTimeLabel = new Label("00:00");
        totalTimeLabel = new Label("00:00");

        // Speed Control Slider
        Slider speedSlider = new Slider(0.5, 2.0, 1.0);
        speedSlider.setBlockIncrement(0.25);
        speedSlider.setPrefWidth(150);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(0.5);
        speedSlider.setMinorTickCount(1);
        speedSlider.getStyleClass().add("custom-slider");

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateSliderTrackFill(speedSlider);
        });
        

        Platform.runLater(() -> updateSliderTrackFill(speedSlider));

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double percent = (newVal.doubleValue() - speedSlider.getMin()) /
                    (speedSlider.getMax() - speedSlider.getMin()) * 100;

            speedSlider.lookup(".track").setStyle(
                    "-fx-background-color: linear-gradient(to right, rgb(238, 138, 75) " + percent + "%, rgba(255, 255, 255, 0.637) "
                            + percent + "%);");
        });

        // Action for Play/Pause Button
        playPauseButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    playPauseButton.setText("▶");
                } else {
                    mediaPlayer.play();
                    playPauseButton.setText("⏸");
                }
            }
        });

        // Action for Rewind Button (-10 seconds)
        rewindButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                Duration currentTime = mediaPlayer.getCurrentTime();
                mediaPlayer.seek(currentTime.subtract(Duration.seconds(10)));
            }
        });

        // Action for Forward Button (+10 seconds)
        forwardButton.setOnAction(e -> {
            if (mediaPlayer != null) {
                Duration currentTime = mediaPlayer.getCurrentTime();
                mediaPlayer.seek(currentTime.add(Duration.seconds(10)));
            }
        });

        // Action for Speed Slider
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setRate(newVal.doubleValue());
            }
        });

        // Action for Progress Slider
        progressSlider.setOnMousePressed(e -> {
            if (mediaPlayer != null)
                mediaPlayer.pause();
        });

        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.seek(Duration.seconds(progressSlider.getValue()));
                mediaPlayer.play();
            }
        });

        if (mediaPlayer != null) {
            mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (!progressSlider.isValueChanging()) {
                    progressSlider.setValue(newTime.toSeconds());
                    currentTimeLabel.setText(formatTime(newTime));
                }
            });

            mediaPlayer.setOnReady(() -> {
                Duration total = mediaPlayer.getMedia().getDuration();
                progressSlider.setMax(total.toSeconds());
                totalTimeLabel.setText(formatTime(total));
            });
        }

        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setPrefWidth(100);

        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null)
                mediaPlayer.setVolume(newVal.doubleValue() / 100);
        });

        progressBox.getChildren().addAll(currentTimeLabel,
                progressSlider, totalTimeLabel);

        timeButtons.getChildren().addAll(prevButton, rewindButton, playPauseButton,
                forwardButton, nextButton);

        timeBox.getChildren().addAll(progressBox, timeButtons);

        Label volumeLabel = new Label("Volume");
        volumeLabel.setPadding(new Insets(0, 0, 0, 30));
        volumeBox.getChildren().addAll( volumeSlider, volumeLabel);

        Label speedLabel = new Label("Speed");
        speedLabel.setPadding(new Insets(0, 0, 0, 50));
        speedBox.getChildren().addAll( speedSlider, speedLabel);

        controlBar.getChildren().addAll(
                timeBox, volumeBox, speedBox );

        return controlBar;
    }

    private Button createStyledButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:  #2b2b2b; -fx-text-fill: white; -fx-padding: 2 4; ");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:rgba(75, 74, 74, 0.44); -fx-text-fill: white; -fx-padding: 2 4; -fx-font-weight: bold;"));
        btn.setOnMouseExited(
                e -> btn.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-padding: 2 4;"));
        return btn;
    }

    private void openMediaFile(Stage stage) {
        File file = new FileChooser().showOpenDialog(stage);
        if (file != null) {
            Media media = new Media(file.toURI().toString());
            if (mediaPlayer != null)
                mediaPlayer.dispose();

            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            mediaPlayer.setOnReady(() -> {
                progressSlider.setMax(mediaPlayer.getTotalDuration().toSeconds());
                totalTimeLabel.setText(formatTime(mediaPlayer.getTotalDuration()));
            });

            mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                progressSlider.setValue(newVal.toSeconds());
                currentTimeLabel.setText(formatTime(newVal));
            });

            mediaPlayer.play();
            playPauseButton.setText("⏸");
        }
    }

    private void toggleMute() {
        if (mediaPlayer != null) {
            if (isMuted) {
                mediaPlayer.setVolume(savedVolume);
                volumeSlider.setValue(savedVolume * 100);
            } else {
                savedVolume = mediaPlayer.getVolume();
                mediaPlayer.setVolume(0);
                volumeSlider.setValue(0);
            }
            isMuted = !isMuted;
        }
    }

    private void updateSliderTrackFill(Slider slider) {
        double min = slider.getMin();
        double max = slider.getMax();
        double value = slider.getValue();
        double percent = (value - min) / (max - min) * 100;
    
        // Lookup track and apply gradient fill
        Region track = (Region) slider.lookup(".track");
        if (track != null) {
            track.setStyle(
                "-fx-background-color: linear-gradient(to right, rgb(238, 138, 75) " + percent + "%, rgba(255, 255, 255, 0.637) " + percent + "%);"
            );
        }
    }
    

    public static void main(String[] args) {
        launch(args);
    }
}