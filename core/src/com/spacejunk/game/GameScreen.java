package com.spacejunk.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.spacejunk.game.constants.GameConstants;
import com.spacejunk.game.levels.Level;
import com.spacejunk.game.menus.RemainingLivesMenu;

public class GameScreen implements Screen {

//	public static boolean DEBUG = true;
	public static boolean DEBUG = false;

	public enum State
	{
		PAUSE,
		RUN,
		CRASHED,
		RESUME,
		STOPPED
	}

	private ShapeRenderer shapeRenderer;
	private SpriteBatch canvas;

	private Texture background;
	private int backgroundImageIndex = 0;

	private Controller controller;

	private BitmapFont font;

	private Texture gameOver;

	private Boolean isGameActive = false;
	private Boolean isCrashed = false;

	private RemainingLivesMenu remainingLivesMenu;

	private SpaceJunk spaceJunk;

	private float elapsedTime;

	private State state;

	public GameScreen(final SpaceJunk game) {
		startGame(game);
	}

	private void startGame(SpaceJunk game) {

		this.spaceJunk = game;
		this.state =  State.RUN;

		this.controller = new Controller(this.spaceJunk);
		this.remainingLivesMenu = new RemainingLivesMenu(this.spaceJunk);

		create();
	}


	public void create () {

		Gdx.app.log("applog", "Create method of gamescreen.java called");

		canvas = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		canvas.enableBlending();

//		background = new Texture("space_background.jpg");
		background = new Texture("background_space_without_bar.jpg");
		background.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

		font = new BitmapFont();
		font.setColor(Color.WHITE);
		font.getData().setScale(7);

		gameOver = new Texture("gameover.png");

		elapsedTime = 0f;
	}


	@Override
	public void dispose () {
		canvas.dispose();
	}


	/**
	 * Prepares an astronaut for rendering. Moves it 'forward' one frame and then draws the result on the canvas
	 * */
	private void renderAstronaut(boolean toAnimate) {

		spaceJunk.getCharacter().updateCharacterPosition();
		spaceJunk.getCharacter().updateCharacterShapeCoordinates();

		if(toAnimate) {
			// Accumulate elapsed animation time only if astronaut is being animated
			elapsedTime += Gdx.graphics.getDeltaTime();
		}

		spaceJunk.getCharacter().render(canvas, elapsedTime, shapeRenderer, toAnimate);
	}

	private void renderObstacles(boolean toMove) {
        spaceJunk.getLevel().renderObstacles(canvas, shapeRenderer, toMove);
        spaceJunk.getLevel().updateObstacleShapeCoordinates();
	}

	private void restartGame() {
		spaceJunk.setUpGame();
		startGame(spaceJunk);
	}

	@Override
	public void render(float delta) {

		switch (state) {
			case RUN:
				renderScreen();
				break;
			case CRASHED:
				renderCrashedScreenEssentials();
				// Game should be restarted now
				if(controller.isTouched()) {
					spaceJunk.getCharacter().resetLives();
					isGameActive = true;
					isCrashed = false;
					restartGame();
					this.state = State.RUN;
				}
				break;
			case PAUSE:
				renderScreenEssentials();
				if(controller.isTouched() && controller.playPauseButtonisPressed()) {
					resume();
				}
				break;
			default:
				break;
		}

	}

	private void renderCrashedScreenEssentials() {
		canvas.begin();
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(Color.GREEN);
		// We are making use of the painters algorithm here
		drawBackground();
		renderController();
		renderAstronaut(false);
		shapeRenderer.setColor(Color.RED);
		renderObstacles(false);
		renderRemainingLives();
		displayScore();
		drawGameOverScreen();
		canvas.end();
		shapeRenderer.end();
	}

	private void renderScreenEssentials() {
		canvas.begin();
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

		// We are making use of the painters algorithm here
		drawBackground();

		renderController();

        shapeRenderer.setColor(Color.GREEN);
		renderAstronaut(false);
		shapeRenderer.setColor(Color.RED);
		renderObstacles(false);

		renderRemainingLives();
		displayScore();
		canvas.end();
		shapeRenderer.end();
	}

	// Note :- Rendering each on screen component that 'moves' updates its internal coordinates
	private void renderScreen() {
		canvas.begin();
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

		// We are making use of the painters algorithm here
		drawBackground();
		// We do this so that the obstacles are painted red
		shapeRenderer.setColor(Color.RED);
		gameLogic();
		// And the astronaut is painted green
		shapeRenderer.setColor(Color.GREEN);
		renderAstronaut(true);
		renderController();
		renderRemainingLives();
		displayScore();

		canvas.end();
		shapeRenderer.end();

		isCrashed = hasCharacterDied();
	}

	private boolean hasCharacterDied() {
		int numberOfObstacles = spaceJunk.getLevel().getObstaclesList().size();

		for(int i = 0; i < numberOfObstacles; i++) {

			if (this
					.spaceJunk
					.getLevel()
					.getObstaclesList()
					.get(i)
					.isBroken())
				continue;

			if(Intersector.overlaps(
					this.spaceJunk.getLevel().getObstaclesList().get(i).getObstacleShape(),
					this.spaceJunk.getCharacter().getCharacterShape())) {

				this.spaceJunk.getLevel().getObstaclesList().get(i).setBroken(true);

				return spaceJunk.getCharacter().takesHit();

			}
		}

		return false;

	}

	private void renderController() {
		controller.render(canvas);
	}

	private void renderRemainingLives() {
		remainingLivesMenu.render(canvas);
	}

	private void gameLogic() {

		if(isGameActive && !isCrashed) {

			spaceJunk.incrementGameScore();

			// There has been on on screen touch action being done
			if(controller.isTouched()) {
				// Check if options menu is interacted with
				if(controller.playPauseButtonisPressed()) {
					pause();
				}
				// If all checks fail, this means the user meant to move the character
				else {
					spaceJunk.getCharacter().moveCharacter(controller.getTouchYCoordinate());
				}
			}

			// Only render and move obstacles if the game is active
			renderObstacles(true);
		}

		else if(!isGameActive && !isCrashed){
			if(controller.isTouched()) {
				isGameActive = true;
			}
		}

		//Code for if crash occurs
		if(isCrashed) {
			this.state = State.CRASHED;
			drawGameOverScreen();
		}

	}


	private void drawBackground() {
		// canvas.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		// Using this method here implies that the background is suitable for wrap as a background
		// Using the width as the "u" parameter implies that image width is greater than screen width
		// If it's not the background is tiled.

		canvas.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), backgroundImageIndex, 0, Gdx.graphics.getWidth(), Math.min(background.getHeight(), Gdx.graphics.getHeight()), false, false);

		if (this.state == State.RUN) {
			backgroundImageIndex += GameConstants.BACKGROUND_SPEED;
		}

		if (backgroundImageIndex > background.getWidth()) {
			backgroundImageIndex = 0;
		}
	}

	private void drawGameOverScreen() {
		canvas.draw(gameOver, Gdx.graphics.getWidth()/2 - gameOver.getWidth()/2, Gdx.graphics.getHeight()/2 - gameOver.getHeight()/2);
	}

	private void displayScore() {
		GlyphLayout layout = new GlyphLayout(font, String.valueOf(spaceJunk.getCurrentGameScore()));
		font.draw(canvas, String.valueOf(spaceJunk.getCurrentGameScore()),
				Gdx.graphics.getWidth() / 2 - layout.width / 2,
				Gdx.graphics.getHeight());
	}


	// Auto generated method stubs
	@Override
	public void show() {

	}


	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void pause() {
		this.state = State.PAUSE;
		controller.getOptionsMenu().setMiddleButtonTextureToPlay();
	}

	@Override
	public void resume() {
		this.state = State.RUN;
		controller.getOptionsMenu().setMiddleButtonTextureToPause();
	}

	@Override
	public void hide() {

	}

}
