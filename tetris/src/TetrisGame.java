
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.paint.Color;

/*
===================================================================
Name		: 테트리스
Author		: 최하늘
Version		: 최종 소스
Copyright	:
Description	: 
===================================================================
*/



public class TetrisGame extends Application{
	
	private Button startBtn = new Button("start");
	private Button pauseBtn = new Button("pause");
	private TetrisGrid mainGrid = new TetrisGrid(22,10);
	private TetrisGrid nextGrid = new TetrisGrid(6,6);
	private TextField levelField = new TextField();
	private TextField lineField = new TextField();
	private TextField scoreField = new TextField();
	private TetrisBlockFactory tetrisBlockFactory = new TetrisBlockFactory();
	private Timeline shoftDropTimer = new Timeline();
	private EventHandler<ActionEvent> btnHandler = new ButtonHandler();
	private Stage stage3D;
	private BorderPane mainPane;
	private int[] framesPerSecond = {48,43,38,33,28,23,18,13,8,6,5,5,5,4,4,4,3,3,3,2};
	private int[] pointsPerLine = {0, 40, 100, 300, 1200};
	
	private int score  = 0;
	private int numberOfLines = 0;
	private int level = 1;

	private int gameState = -1;				// -1 초기상태 / 0 게임중 / 1 일시정지 / 2 게임오버
	private boolean hardDropBonus = false;	// 하드드롭 보너스를 실시간이 아닌 타이머 핸들에서 주게 하기 위한 변수
	
	private class TimerHandler implements EventHandler<ActionEvent>{
		private boolean active = true;
		@Override
		public void handle(ActionEvent event) {
			active = mainGrid.moveShapeDown();
			//mainGrid.repaint();
			repaint3D();
			mainGrid.requestFocus();
			
			// 블록이 바닥에 닫았을 시
			if(!active){
				// 바닥에 닫은 뒤 커맨드 입력 유예 시간 0.25초
				// 여기까지
				
				// 완성된 라인 지우고 점수 적용
				int removedLines = mainGrid.removeFullRow();
				numberOfLines += removedLines;
				switch(removedLines){
				case 1: Sound.play("SingleLineClear"); break;
				case 2: Sound.play("DoubleLineClear"); break;
				case 3: case 4: Sound.play("TripleLineClear"); break;
				}
				score+=pointsPerLine[removedLines]*(level);
				// 여기까지
				
				// 레벨을 계산하고 레벨에 맞게 타이머 속도 변환
				levelUp();
				
				// StatePane 최신화
				levelField.setText(level+"");
				lineField.setText(numberOfLines+"");
				scoreField.setText(score+"");
				
				// 드롭 점수 계산
				if(hardDropBonus) {
					hardDropBonus = false;
					score+=8;
				}
				else score +=4;
				// 여기까지
				
				// 다음 블록 생성 준비
				tetrisBlockFactory.changeBlock();
				
				// 다음 블록을 생성하되 생성할 자리가 없으면 게임오버
				if(!startNewBlock()) {
					// gameOver
					gameState = 2;
					shoftDropTimer.stop();
					Platform.runLater(new Runnable() {
		                @Override
		                public void run() {
		                	HallOfFame dialog = new HallOfFame();
		                	dialog.show(score);
		                }
					}); // TimerLine Handling 중에는 dialog showAndWait 실행 못함
				}
				active = true;
			}
		}
	}
	
	private class KeyboardHandler implements EventHandler<KeyEvent>{
		@Override
		public void handle(KeyEvent event) {	
			if(gameState == 0) {
				switch(event.getCode()){
				case LEFT: mainGrid.moveShapeLeft(); event.consume(); break;
				case RIGHT: mainGrid.moveShapeRight(); event.consume(); break;
				case UP: mainGrid.rotateShape(); event.consume(); break;
				case DOWN: mainGrid.moveShapeDown(); event.consume(); break;
				case SPACE: 
					mainGrid.moveShapeToBottom();
					event.consume(); 
					hardDropBonus = true;
					break;
				}
			}
			//mainGrid.repaint();
			repaint3D();
			mainGrid.requestFocus();
		}
	}
	
	private class ButtonHandler implements EventHandler<ActionEvent>{
		@Override
		// gameState == -1 초기상태 / 0 게임중 / 1 일시정지 / 2 게임오버
		public void handle(ActionEvent event) {
			Object source = event.getSource();
			// case : start
			if (source == startBtn && gameState == -1) {
				gameState = 0;
				startNewBlock();
				mainGrid.requestFocus();
				shoftDropTimer.play();
			}
			// case : continue
			else if (source == startBtn && gameState == 1 && gameState != 2) {
				gameState = 0;
				mainGrid.requestFocus();
				shoftDropTimer.play();
			}
			// case : reStart
			else if(source == startBtn && (gameState == 0 || gameState == 2)) {
				gameState = 0;
				score  = 0;
				numberOfLines = 0;
				level = 1;
				levelField.setText(level+"");
				lineField.setText(numberOfLines+"");
				scoreField.setText(score+"");
				
				mainGrid.clear();
				startNewBlock();
				mainGrid.requestFocus();
				shoftDropTimer.play();
			}
			// pause
			else if(source==pauseBtn && gameState != 2){
				gameState = 1;
				shoftDropTimer.stop();
			}
		}
	}
	
	private HBox createActionPane(){
		HBox actionBox = new HBox();
		actionBox.setSpacing(10);
		actionBox.setPadding(new Insets(20,0,10,0));
		actionBox.setAlignment(Pos.CENTER);
		startBtn.setMinWidth(80);
		pauseBtn.setMinWidth(80);
		startBtn.setOnAction(btnHandler);
		pauseBtn.setOnAction(btnHandler);
		actionBox.getChildren().addAll(startBtn, pauseBtn);
		return actionBox;
	}
	
	private VBox createStatePane(){
		VBox stateBox = new VBox();
		stateBox.setSpacing(10);
		stateBox.setPadding(new Insets(10,20,0,0));
		stateBox.setAlignment(Pos.TOP_CENTER);
		Label levelLabel = new Label("level");
		Label lineLabel = new Label("line");
		Label scoreLabel = new Label("score");
		levelField.setMaxWidth(120);
		lineField.setMaxWidth(120);
		scoreField.setMaxWidth(120);
		levelField.setEditable(false);
		lineField.setEditable(false);
		scoreField.setEditable(false);
		
		levelField.setText(level+"");
		lineField.setText(numberOfLines+"");
		scoreField.setText(score+"");
		
		VBox.setMargin(nextGrid, new Insets(0,0,80,0));
		stateBox.getChildren().addAll(nextGrid,levelLabel,levelField,
				lineLabel,lineField,scoreLabel,scoreField);
		return stateBox;
	}
	
	private Scene createMainTetrisScene(){
		mainPane = new BorderPane();
		
		VBox mainBox = new VBox();
		mainBox.setPadding(new Insets(200,0,0,20));
		mainBox.getChildren().add(mainGrid);
		mainGrid.setOnKeyPressed(new KeyboardHandler());
		
		mainPane.setBottom(mainBox);
		mainPane.setTop(createActionPane());
		mainPane.setRight(createStatePane());
		
		Group group = new Group();
		for (int i = 19; i >= 0; i--) {
			for (int j = 0; j < 10; j++) {
				Image img;
				if (mainGrid.getColor(i, j) != Color.DARKGRAY) {
					img = new Image(getClass().getResourceAsStream("RED.png"));
				}
				else {
					img = new Image(getClass().getResourceAsStream("EMPTY.png"));
				}
					ImageView iView = new ImageView(img);
					iView.setX(j * 17);
					iView.setY(i * 16);
					iView.setOpacity(1);
					iView.setPreserveRatio(true);
					group.getChildren().addAll(iView);
				
			}
		}
		Scene scene = new Scene(group, 600, 600);
		mainPane.setCenter(group);
		
		return new Scene(mainPane, 380, 540); //540
		}

	@Override
	public void start(Stage primaryStage) throws Exception {		
		shoftDropTimer.setCycleCount(Animation.INDEFINITE);
		shoftDropTimer.getKeyFrames().add(new KeyFrame(Duration.millis((double)1000*framesPerSecond[level-1]/60),new TimerHandler()));
		//shoftDropTimer.setDelay(Duration.millis(500));
		
		stage3D = primaryStage;
		primaryStage.setTitle("Java Tetris");
		primaryStage.setScene(createMainTetrisScene());
		primaryStage.show();
	}
	
	public boolean startNewBlock(){
		boolean canInsert;
		TetrisBlockShape currShape = tetrisBlockFactory.getCurrent();
		canInsert = mainGrid.insertShape(currShape);
		//mainGrid.repaint();
		repaint3D();
		mainGrid.requestFocus();
		
		nextGrid.clear();
		nextGrid.insertNextShape(tetrisBlockFactory.getNext());
		nextGrid.repaint();
		
		return canInsert;
	}
	public void repaint3D() {
		mainPane = new BorderPane();
		VBox mainBox = new VBox();
		mainBox.setPadding(new Insets(200,0,0,20));
		mainBox.getChildren().add(mainGrid);
		mainGrid.setOnKeyPressed(new KeyboardHandler());
		
		mainPane.setBottom(mainBox);
		mainPane.setTop(createActionPane());
		mainPane.setRight(createStatePane());
		
		Group group = new Group();
		for (int i = 22; i >= 0; i--) {
			for (int j = -1; j < 11; j++) {
				Image img;
				if ( i==22 || (j==-1 || j==10) )
					img = new Image(getClass().getResourceAsStream("GRAY.png"));
				else if (mainGrid.getColor(i, j) == Color.YELLOW)
					img = new Image(getClass().getResourceAsStream("YELLOW.png"));		
				else if (mainGrid.getColor(i, j) == Color.MAGENTA)
					img = new Image(getClass().getResourceAsStream("RED.png"));			
				else if (mainGrid.getColor(i, j) == Color.GREEN)
					img = new Image(getClass().getResourceAsStream("GREEN.png"));			
				else if (mainGrid.getColor(i, j) == Color.ORANGE)
					img = new Image(getClass().getResourceAsStream("ORANGE.png"));
				else if (mainGrid.getColor(i, j) == Color.BLUE)
					img = new Image(getClass().getResourceAsStream("BLUE.png"));		
				else if (mainGrid.getColor(i, j) == Color.CYAN)
					img = new Image(getClass().getResourceAsStream("CYAN.png"));			
				else if (mainGrid.getColor(i, j) == Color.PINK)
					img = new Image(getClass().getResourceAsStream("VIOLET.png"));
				else 
					img = new Image(getClass().getResourceAsStream("EMPTY.png"));
					ImageView iView = new ImageView(img);
					iView.setX(j * 17);
					iView.setY(i * 16);
					iView.setOpacity(1);
					iView.setPreserveRatio(true);
					group.getChildren().addAll(iView);
			}
		}
		mainPane.setCenter(group);
		//Stage old = stage3D;
		//stage3D = new Stage();
		stage3D.setScene(new Scene(mainPane, 380, 540));
	}
	public void levelUp(){
		level = (numberOfLines/10)+1;
		shoftDropTimer.stop();
		shoftDropTimer.getKeyFrames().clear();
		shoftDropTimer.getKeyFrames().add(new KeyFrame(Duration.millis((double)1000*framesPerSecond[level-1]/60),new TimerHandler()));
		shoftDropTimer.play();
	}
	
	public static void main(String[] args) {
		Application.launch(args);
	}
}
