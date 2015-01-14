/*
 * Copyright (C) 2015 Andrey Rychkov <wholegroup@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wholegroup.android.knight;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View
{
	/** Процент от реальной ширины поля. Используется для отрисовки рамки. */
	private final static int FIELD_PERCENT = 90;

	/** Процент от размера стороны клетки. Используется для отрисовки фишки занятой клетки. */
	private final static int CELL_PERCENT = 70;

	/** Ширина. */
	private int mWidth;

	/** Половина ширины области вывода (кэш, float). */
	private float mWidth2f;

	/** Высота. */
	private int mHeight;

	/** Половина высоты области вывода. */
	private float mHeight2f;

	/** Игра. */
	private Game mGame;

	/** Уровень игры. */
	private int mLevel;

	/** Игровое поле. */
	private Bitmap mFieldBitmap;

	/** Координата X вывода поля. */
	private int mFieldX;

	/** Координата Y вывода поля. */
	private int mFieldY;

	/** Размер клетки. */
	private int mCellSize;

	/** Размер поля по горизонтали. */
	private int mFieldSizeX;

	/** Размер поля по вертикали. */
	private int mFieldSizeY;

	/** Ширина рамки вокруг поля. */
	private int mBorderSize;

	/** Конь. */
	private Bitmap mKnightBitmap;

	/** Битмап занятой клетки. */
	private Bitmap mBusyBitmap;

	/** Битмап выделенной клетки. */
	private Bitmap mSelectBitmap;

	/** Координата X выделенной клетки. */
	private int mSelectedX;

	/** Координата Y выделенной клетки. */
	private int mSelectedY;

	/** Битмап подсказки. */
	private Bitmap mHintBitmap;

	/** Обработчик событий ячеек. */
	private ICellListener mCellListener;

	/** Интерфейс обработчика событий ячеек. */
	public interface ICellListener
	{
		abstract void onCellSelected(final int x, final int y);

		abstract void onTouchEvent(MotionEvent event);
	}

	/**
	 * Установка обработчика событий ячеек.
	 *
	 * @param cellListener обработчик событий ячеек
	 */
	public void setCellListener(ICellListener cellListener)
	{
		mCellListener = cellListener;
	}

	/**
	 * Устанавливает игру для отображения.
	 *
	 * @param game объект игры
	 */
	public void setGame(Game game)
	{
		mGame = game;

		// освобождение ресурсов т.к. игра поменялась (размер и прочие параметры)
		freeResources();
	}

	/**
	 * Установка уровня игры.
	 *
	 * @param level уровень игры
	 */
	public void setLevel(final int level)
	{
		mLevel = level;
	}

	/**
	 * Конструктор.
	 *
	 * @param context контекст
	 * @param attrs атрибуты
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public GameView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}
	
	/**
	 * Отрисовка.
	 *
	 * @param canvas канвас для рисования
	 */
	@Override
	protected void onDraw(Canvas canvas)
	{
		// отрисовка родителя
		super.onDraw(canvas);

		// генерация битмапов
		if (null == mFieldBitmap)
		{
			initResources();
		}

		// вывод поля
		canvas.drawBitmap(mFieldBitmap, 0, 0, null);

		// вывод клеток поля
		final int gameSizeX = mGame.getFieldSizeX();
		final int gameSizeY = mGame.getFieldSizeY();
		final int knightX   = mGame.getX();
		final int knightY   = mGame.getY();

		for (int y = 0; y < gameSizeY; y++)
		{
			final int dy = mFieldY + y * mCellSize;
			
			for (int x = 0; x < gameSizeX; x++)
			{
				final int dx = mFieldX + x * mCellSize;

				// поле
				switch (mGame.getCell(x, y))
				{
					//
					case Game.Cell.LIKE:
					{
						if (0 == mLevel)
						{
							canvas.drawBitmap(mHintBitmap, dx, dy, null);
						}

						break;
					}

					//
					case Game.Cell.BUSY:
					{
						canvas.drawBitmap(mBusyBitmap, dx, dy, null);

						break;
					}

					default:
						break;
				}

				// вывод коня
				if ((x == knightX) && (y == knightY))
				{
					canvas.drawBitmap(mKnightBitmap, dx, dy, null);
				}

				// выделенная клетка
				if ((x == mSelectedX) && (y == mSelectedY))
				{
					canvas.drawBitmap(mSelectBitmap, dx, dy, null);
				}
			}
		}
		
		// отрисовка слоя окончания игры
		if (mGame.isEnd())
		{
			drawEndGame(canvas);
		}
	}

	/**
	 * Обработка событий тача.
	 *
	 * @param event событие
	 *
	 * @return true, в случае успеха
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// обработка нажатия
		if ((MotionEvent.ACTION_DOWN == event.getAction()) && (null != mCellListener))
		{
			// если игра закончена, то перенаправляем событие активити
			if (mGame.isEnd())
			{
				mCellListener.onTouchEvent(event);
			}

			// иначе обрабатываем выбор клетки
			else
			{
				final float xf = (event.getX() - mFieldX) / mCellSize;
				final float yf = (event.getY() - mFieldY) / mCellSize;

				boolean endLoop = false;

				while (!endLoop)
				{
					// выход за приделы массива
					if ((0 > xf) || (0 > yf))
					{
						break;
					}

					if ((mGame.getFieldSizeX() <= xf) || (mGame.getFieldSizeY() <= yf))
					{
						break;
					}

					// вызываем обработчик
					mCellListener.onCellSelected((int)xf, (int)yf);

					endLoop = true;
				}
			}
		}

		return super.onTouchEvent(event);
	}
		
	/**
	 * Устанавливает координаты выделенной клетки.
	 *
	 * @param x координата X
	 * @param y координата Y
	 */
	public void setSelected(final int x, final int y)
	{
		mSelectedX = x;
		mSelectedY = y;
	}

	/**
	 * Освобождает память занимаемую различными ресурсами.
	 */
	public void freeResources()
	{
		//
		if (null != mFieldBitmap)
		{
			mFieldBitmap.recycle();

			mFieldBitmap = null;
		}

		//
		if (null != mKnightBitmap)
		{
			mKnightBitmap.recycle();

			mKnightBitmap = null;
		}

		//
		if (null != mSelectBitmap)
		{
			mSelectBitmap.recycle();

			mSelectBitmap = null;
		}

		//
		if (null != mHintBitmap)
		{
			mHintBitmap.recycle();

			mHintBitmap = null;
		}

		if (null != mBusyBitmap)
		{
			mBusyBitmap.recycle();

			mBusyBitmap = null;
		}
	}

	/** */
	private void initResources()
	{
		// вычисление различных значений
		calculateValues();

		// отрисовка поля
		drawFieldBitmap();

		// отрисовка коня
		drawKnightBitmap();

		// отрисовка битмапа выделения
		drawSelectedBitmap();

		// отрисовка битмапа подсказки
		drawHintBitmap();

		// отрисовка битмапа занятой клетки
		drawBusyBitmap();
	}

	/**
	 * Отрисовка битмапа подсказки.
	 */
	private void drawHintBitmap()
	{
		if (null != mHintBitmap)
		{
			mHintBitmap.recycle();
		}

		mHintBitmap = Bitmap.createBitmap(mKnightBitmap.getWidth(),
			mKnightBitmap.getHeight(), Bitmap.Config.ARGB_8888);

		final Canvas canvas = new Canvas(mHintBitmap);
		final Paint  paint  = new Paint();

		paint.setAlpha(0x20);

		canvas.drawBitmap(mKnightBitmap, 0, 0, paint);
	}

	/**
	 * Вычисляет различные значения.
	 */
	private void calculateValues()
	{
		// логический размер игрового поля
		final int gameSizeX = mGame.getFieldSizeX();
		final int gameSizeY = mGame.getFieldSizeY();

		// размеры экрана
		mWidth  = getWidth();
		mHeight = getHeight();

		mWidth2f  = mWidth / 2.0f;
		mHeight2f = mHeight / 2.0f;
		
		// размер клетки
		int cellSizeX = (mWidth * FIELD_PERCENT / 100) / gameSizeX;
		int cellSizeY = (mHeight * FIELD_PERCENT / 100) / gameSizeY;

		mCellSize = cellSizeX < cellSizeY ? cellSizeX : cellSizeY;

		// ширина рамки
		mBorderSize = (cellSizeX < cellSizeY ? mWidth : mHeight) * (100 - FIELD_PERCENT) / 400;

		// размер поля в пикселах
		mFieldSizeX = mCellSize * gameSizeX;
		mFieldSizeY = mCellSize * gameSizeY;

		// центрируем
		mFieldX = (mWidth - mFieldSizeX) / 2;
		mFieldY = (mHeight - mFieldSizeY) / 2;
	}

	/**
	 * Отрисовка битмапа поля.
	 */
	private void drawFieldBitmap()
	{
		// удаляем старый битмап
		if (null != mFieldBitmap)
		{
			mFieldBitmap.recycle();
		}

		// светлая клетка
		final Bitmap lightBitmap = Bitmap.createBitmap(mCellSize, mCellSize, Bitmap.Config.ARGB_8888);
		final Canvas lightCanvas = new Canvas(lightBitmap);
		final Paint  lightPaint  = new Paint();

		lightPaint.setShader(
			new BitmapShader(
				BitmapFactory.decodeResource(
					getResources(),
					R.drawable.light
				),
				Shader.TileMode.REPEAT,
				Shader.TileMode.REPEAT
			)
		);
		
		lightCanvas.drawRect(0, 0, mCellSize, mCellSize, lightPaint);

		// темная клетка
		final Bitmap darkBitmap = Bitmap.createBitmap(mCellSize, mCellSize, Bitmap.Config.ARGB_8888);
		final Canvas darkCanvas = new Canvas(darkBitmap);
		final Paint  darkPaint  = new Paint();

		darkPaint.setShader(
			new BitmapShader(
				BitmapFactory.decodeResource(
					getResources(),
					R.drawable.dark
				),
				Shader.TileMode.REPEAT,
				Shader.TileMode.REPEAT
			)
		);

		darkCanvas.drawRect(0, 0, mCellSize, mCellSize, darkPaint);

		// поле
		mFieldBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);

		final Canvas canvasField = new Canvas(mFieldBitmap);

		// ссылка на ресурсы
		final Resources res = getResources();

		// отрисовка рамки
		final Paint paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
		final Path  pathBorder  = new Path();

		pathBorder.reset();
		pathBorder.addRect(mFieldX - mBorderSize, mFieldY - mBorderSize,
			mFieldX + mFieldSizeX + mBorderSize, mFieldY + mFieldSizeY + mBorderSize,
			Path.Direction.CW);

		paintBorder.setColor(res.getColor(R.color.field_border_fg));
		canvasField.drawPath(pathBorder, paintBorder);

		pathBorder.reset();
		pathBorder.addRect(mFieldX - mBorderSize + 1, mFieldY - mBorderSize + 1,
			mFieldX + mFieldSizeX + mBorderSize - 1, mFieldY + mFieldSizeY + mBorderSize - 1,
			Path.Direction.CW);

		paintBorder.setColor(res.getColor(R.color.field_border_bg));
		canvasField.drawPath(pathBorder, paintBorder);

		pathBorder.reset();
		pathBorder.addRect(mFieldX - 1, mFieldY - 1, mFieldX + mFieldSizeX + 1,
			mFieldY + mFieldSizeY + 1, Path.Direction.CW);

		paintBorder.setColor(res.getColor(R.color.field_border_fg));
		canvasField.drawPath(pathBorder, paintBorder);

		// закрашиваем клетками поле
		final int gameSizeX = mGame.getFieldSizeX();
		final int gameSizeY = mGame.getFieldSizeY();

		boolean isDark = true;
		
		for (int y = 0; y < gameSizeY; y++)
		{
			final int posY = mFieldY + y * mCellSize;

			for (int x = 0; x < gameSizeX; x++)
			{
				final int posX = mFieldX + x * mCellSize;

				canvasField.drawBitmap(isDark ? darkBitmap : lightBitmap, posX, posY, null);

				isDark = !isDark;
			}

			if (0 == gameSizeX % 2)
			{
				isDark = !isDark;
			}
		}

		lightBitmap.recycle();
		darkBitmap.recycle();			
	}

	/**
	 * Отрисовка битмапа коня.
	 */
	private void drawKnightBitmap()
	{
		// удаляем старый битмап
		if (null != mKnightBitmap)
		{
			mKnightBitmap.recycle();
		}

		// ресурсы
		final Resources res = getResources();

		// битмап
		mKnightBitmap = Bitmap.createBitmap(mCellSize, mCellSize, Bitmap.Config.ARGB_8888);

		// канван
		final Canvas canvas = new Canvas(mKnightBitmap);

		// кисть
		final Paint paint  = new Paint(Paint.ANTI_ALIAS_FLAG);

		paint.setTextSize(mCellSize * FIELD_PERCENT / 100);
		paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setStrokeWidth(1);

		// тело
		paint.setColor(res.getColor(R.color.knight_color));
		paint.setShader(
			new LinearGradient(
				0, 0, mCellSize / 2.0f, 0,
				res.getColor(R.color.knight_gradient_start), res.getColor(R.color.knight_gradient_stop),
				Shader.TileMode.MIRROR
			)
		);

		canvas.drawText("♞", 0, 1, mCellSize / 2.0f,
			(mCellSize - (paint.ascent() + paint.descent())) / 2.0f, paint);

		// обводка
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(res.getColor(R.color.knight_border));
		paint.setShader(null);

		canvas.drawText("♞", 0, 1, mCellSize / 2.0f,
			(mCellSize - (paint.ascent() + paint.descent())) / 2.0f, paint);
	}

	/**
	 * Отрисовка битмапа выделенной клетки.
	 */
	private void drawSelectedBitmap()
	{
		// освобождение памяти
		if (null != mSelectBitmap)
		{
			mSelectBitmap.recycle();
		}

		// ресурсы
		final Resources res = getResources();

		// битмап
		mSelectBitmap = Bitmap.createBitmap(mCellSize, mCellSize, Bitmap.Config.ARGB_8888);

		// канвас
		final Canvas canvas = new Canvas(mSelectBitmap);
		
		// закрашиваем
		canvas.drawColor(res.getColor(R.color.selected));
	}

	/**
	 * Отрисовывает слой окончания игры.
	 *
	 * @param canvas канвас для отрисовки
	 */
	private void drawEndGame(final Canvas canvas)
	{
		// ссылка на ресурсы
		final Resources res = getResources();

		// затеняем фон
		canvas.drawColor(res.getColor(R.color.blackout));

		// текст надписи
		final String text = getResources().getString(
			mGame.isSolved() ? R.string.solved : R.string.gameover
		);

		// кисть для надписи
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

		paint.setTextSize(mWidth / text.length() * 1.5f);
		paint.setStrokeWidth(2);
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

		// надпись
		paint.setColor(res.getColor(R.color.endgame_text));
		paint.setStyle(Paint.Style.FILL);

		canvas.drawText(text, mWidth2f, mHeight2f - (paint.ascent() + paint.descent()) / 2.0f, paint);

		// обводка
		paint.setColor(res.getColor(R.color.endgame_text_border));
		paint.setStyle(Paint.Style.STROKE);

		canvas.drawText(text, mWidth2f, mHeight2f - (paint.ascent() + paint.descent()) / 2.0f, paint);
	}

	/**
	 * Отрисовка битмапа занятой клетки.
	 */
	private void drawBusyBitmap()
	{
		// удаляем старый битмап
		if (null != mBusyBitmap)
		{
			mBusyBitmap.recycle();
		}

		// ссылка на ресурсы
		final Resources res = getResources();

		// битмап
		mBusyBitmap = Bitmap.createBitmap(mCellSize, mCellSize, Bitmap.Config.ARGB_8888);

		// канвас
		final Canvas canvas = new Canvas(mBusyBitmap);

		// шарик
		final float cellSize2f = mCellSize / 2.0f;
		final float radius     = cellSize2f * CELL_PERCENT / 100.0f;
		final float radius2    = radius / 2.0f;

		final Path pathBusy = new Path();

		pathBusy.addCircle(cellSize2f, cellSize2f, radius, Path.Direction.CW);

		// закраска градиентом
		final Paint paintBusy = new Paint(Paint.ANTI_ALIAS_FLAG);

		paintBusy.setColor(Color.WHITE);

		paintBusy.setShader(
			new RadialGradient(
				cellSize2f  - radius2, cellSize2f  - radius2, radius * 2,
				res.getColor(R.color.busy_gradient_start), res.getColor(R.color.busy_gradient_stop),
				Shader.TileMode.CLAMP
			)
		);

		canvas.drawPath(pathBusy, paintBusy);

		// обводка шарика
		paintBusy.setColor(res.getColor(R.color.busy_border));
		paintBusy.setShader(null);
		paintBusy.setStyle(Paint.Style.STROKE);
		paintBusy.setStrokeWidth(1);

		canvas.drawPath(pathBusy, paintBusy);
	}

	/**
	 * Обработка изменения размера экрана.
	 *
	 * @param width     ширина
	 * @param height    высота
	 * @param widthOld  старая ширина
	 * @param heightOld старая высота
	 */
	@Override
	protected void onSizeChanged(int width, int height, int widthOld, int heightOld)
	{
		// освобожаем память занимаемую битмапами
		freeResources();

		super.onSizeChanged(width, height, widthOld, heightOld);
	}
}
