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

import android.util.Log;
import biz.source_code.base64Coder.Base64Coder;

import java.io.*;

public class Game
{
	/** Наименование класса для лога. */
	public static final String TAG = "Game";
	
	/** Позиция коня по X. */
	private int mX;

	/** Позиция коня по Y. */
	private int mY;

	/** Размер поля - количество клеток по горизонтали. */
	private int mFieldSizeX;

	/** Размер поля - количество клетов по вертикали. */
	private int mFieldSizeY;

	/** Игровое поле. */
	private int[][] mField;

	/** Значения клеток поля. */
	public interface Cell
	{
		/** Пустая клетка. */
		public final int EMPTY = 0;

		/** Клетка уже занята. */
		public final int BUSY  = 1;

		/** Возможно следующий ход. */
		public final int LIKE  = 2;
	}

	/** Размеры доски по уровням. */
	private final static int[][] mLevelValues = {{4, 3}, {5, 4}, {5, 5}, {6, 4}, {7, 3}, {8, 8}};

	/** Количество сделанных ходов. */
	private int mMoveCount;

	/** Флаг решения головоломки. */
	private boolean mSolved;

	/** Флаг окочания игры. */
	private boolean mGameOver;

	/**
	 * Конструктор.
	 */
	public Game(final int level)
	{
		create(level);
	}

	/**
	 * Инициализация игры.
	 */
	public void create(final int level)
	{
		// флаг окончания игры
		mGameOver = false;

		// флаг нерешенности головоломки
		mSolved = false;

		// обнуляем число ходов
		mMoveCount = 0;

		// размер поля
		final int levelSelect;

		if (0 > level)
		{
			levelSelect = 0;
		}
		else if (mLevelValues.length <= level)
		{
			levelSelect = mLevelValues.length - 1;
		}
		else
		{
			levelSelect = level;
		}

		mFieldSizeX = mLevelValues[levelSelect][0];
		mFieldSizeY = mLevelValues[levelSelect][1];

		// инициализация поля
		mField = new int[mFieldSizeY][mFieldSizeX];

		for (int y = 0; y < mFieldSizeY; y++)
		{
			for (int x = 0; x < mFieldSizeX; x++)
			{
				mField[y][x] = Cell.EMPTY;
			}
		}

		// начальная позиция коня
		mX = -1;
		mY = -1;
	}
	
	/**
	 * Выполняем ход по указанным координатам.
	 *
	 * @param x кордината X
	 * @param y координата Y
	 *
	 * @return true, в случае успеха
	 */
	public boolean moveTo(final int x, final int y)
	{
		// выход, если игра не запущена
		if (isEnd())
		{
			return false;
		}
		
		// сдвигаем коня
		if (!moveKnight(x, y))
		{
			return false;
		}

		// увеличиваем количество ходов
		mMoveCount++;

		// проверяем решение
		if (checkSolved())
		{
			mSolved = true;
		}

		// проверяем возможность сделать ход
		if (!mSolved && !checkExistMove())
		{
			mGameOver = true;
		}

		// установка на поле возможных позиций
		setLikePosition();

		return true;
	}

	/**
	 * Проверка решения.
	 *
	 * @return true, если головоломка решена
	 */
	private boolean checkSolved()
	{
		for (int y = 0; y < mFieldSizeY; y++)
		{
			for (int x = 0; x < mFieldSizeX; x++)
			{
				// место, где стоит конь, считается занятым
				if ((x == mX) && (y == mY))
				{
					continue;
				}

				// если клетка не занята и там нет фигуры коня, то игра не решена
				if (Cell.BUSY != mField[y][x])
				{
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Перемещает коня в указанные координаты.
	 *
	 * @param x координата X
	 * @param y координата Y
	 *
	 * @return true, если перемещение выполнено
	 */
	private boolean moveKnight(final int x, final int y)
	{
		// проверка выхода за границы массива
		if ((0 > x) || (0 > y))
		{
			return false;
		}

		if ((mFieldSizeX <= x) || (mFieldSizeY <= y))
		{
			return false;
		}

		// клетка должна быть не занята
		if (Cell.BUSY == mField[y][x])
		{
			return false;
		}

		// на первом ходе сразу устанавливаем коня
		if ((0 > mX) && (0 > mY))
		{
			mX = x;
			mY = y;
			
			return true;
		}

		// проверка пути коня
		final int diffX = Math.abs(mX - x);
		final int diffY = Math.abs(mY - y);

		if (((1 == diffX) && (2 == diffY)) || ((2 == diffX) && (1 == diffY)))
		{
			// предыдущая клетка становится занятой
			mField[mY][mX] = Cell.BUSY;

			// новая позиция коня
			mX = x;
			mY = y;

			return true;
		}

		return false;
	}

	/**
	 * @return флаг окончания игры.
	 */
	public boolean isGameOver()
	{
		return mGameOver;
	}

	/**
	 * @return флаг решения головоломки.
	 */
	public boolean isSolved()
	{
		return mSolved;
	}

	/**
	 * @return флаг окончания игры.
	 */
	public boolean isEnd()
	{
		return isGameOver() || isSolved();
	}

	/**
	 * @return позиция коня X.
	 */
	public int getX()
	{
		return mX;
	}

	/**
	 * @return позиция коня Y.
	 */
	public int getY()
	{
		return mY;
	}

	/**
	 * @return количество клеток по горизонтали.
	 */
	public int getFieldSizeX()
	{
		return mFieldSizeX;
	}

	/**
	 * @return количество клеток по вертикали.
	 */
	public int getFieldSizeY()
	{
		return mFieldSizeY;
	}

	/**
	 * Возвращает значение клетки поля.
	 *
	 * @param x координата X
	 * @param y координата Y
	 *
	 * @return значение клетки поля
	 */
	public int getCell(final int x, final int y)
	{
		return mField[y][x];
	}

	/**
	 * Проверяет наличие ходов.
	 *
	 * @return true, если ход существует
	 */
	private boolean checkExistMove()
	{
		final int[] checkX = {1, 2, 1, 2, -1, -2, -1, -2};
		final int[] checkY = {2, 1, -2, -1, 2, 1, -2, -1};

		for(int i = 0; i < 8; i++)
		{
			// позиция для проверки
			final int cx = mX + checkX[i];
			final int cy = mY + checkY[i];

			// проверка выхода за границу
			if ((cx < 0) || (cy < 0))
			{
				continue;
			}

			if ((mFieldSizeX <= cx) || (mFieldSizeY <= cy))
			{
				continue;
			}

			// клетка должна быть не занятой
			if (Cell.BUSY != mField[cy][cx])
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Устанавливает возможные позиции.
	 */
	private void setLikePosition()
	{
		// очистка от старых рекомендаций
		for (int y = 0; y < mFieldSizeY; y++)
		{
			for (int x = 0; x < mFieldSizeX; x++)
			{
				if (Cell.LIKE == mField[y][x])
				{
					mField[y][x] = Cell.EMPTY;
				}
			}
		}

		// установка
		final int[] checkX = {1, 2, 1, 2, -1, -2, -1, -2};
		final int[] checkY = {2, 1, -2, -1, 2, 1, -2, -1};

		for (int i = 0; i < 8; i++)
		{
			// позиция для проверки
			final int cx = mX + checkX[i];
			final int cy = mY + checkY[i];

			// проверка выхода за границу
			if ((cx < 0) || (cy < 0))
			{
				continue;
			}

			if ((mFieldSizeX <= cx) || (mFieldSizeY <= cy))
			{
				continue;
			}

			// клетка должна быть не занятой
			if (Cell.BUSY != mField[cy][cx])
			{
				mField[cy][cx] = Cell.LIKE;
			}
		}
	}

	/**
	 * Серилизует данные в строку Base64.
	 *
	 * @return строка в кодировке Base64
	 */
	public String toBase64()
	{
		try
		{
			// серилизуем массив
			final ByteArrayOutputStream os  = new ByteArrayOutputStream();
			final ObjectOutputStream    out = new ObjectOutputStream(os);

			out.writeObject(mField);
			out.writeInt(mX);
			out.writeInt(mY);
			out.writeBoolean(mSolved);
			out.writeBoolean(mGameOver);
			out.writeInt(mMoveCount);

			out.close();

			return new String(Base64Coder.encode(os.toByteArray()));
		}
		catch (IOException exception)
		{
			Log.e(TAG, exception.toString());
		}

		return "";
	}

	/**
	 * Восстанавливает данные объекта из строки в Base64.
	 *
	 * @param data строка в Base64
	 */
	public void fromBase64(String data)
	{
		try
		{
			final ByteArrayInputStream is = new ByteArrayInputStream(Base64Coder.decode(data));
			final ObjectInputStream    in = new ObjectInputStream(is);

			final int[][] gameField = (int[][])in.readObject();
			
			mX          = in.readInt();
			mY          = in.readInt();
			mSolved     = in.readBoolean();
			mGameOver   = in.readBoolean();
			mMoveCount  = in.readInt();

			in.close();

			// копируем массив
			for (int y = 0 ; y < mFieldSizeY; y++)
			{
				System.arraycopy(gameField[y], 0, mField[y], 0, mFieldSizeX);
			}
		}
		catch (ArrayIndexOutOfBoundsException exception)
		{
			Log.e(TAG, exception.toString());

			for (int y = 0; y < mFieldSizeY; y++)
			{
				for (int x = 0; x < mFieldSizeX; x++)
				{
					mField[y][x] = Cell.EMPTY;
				}
			}
		}
		catch (ClassNotFoundException exception)
		{
			Log.e(TAG, exception.toString());
		}
		catch (IOException exception)
		{
			Log.e(TAG, exception.toString());
		}

		//
		if ((0 > mX) || (mFieldSizeX <= mX))
		{
			mX = -1;
		}

		if ((0 > mY) || (mFieldSizeY <= mY))
		{
			mY = -1;
		}
	}
}
