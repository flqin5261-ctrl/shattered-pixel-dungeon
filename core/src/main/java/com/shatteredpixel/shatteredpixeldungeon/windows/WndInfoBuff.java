/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;

public class WndInfoBuff extends Window {

	private static final float GAP	= 2;

	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 160;

	public WndInfoBuff(Buff buff){
		super();

		int width = Math.min(WIDTH_MAX, Math.max(WIDTH_MIN, PixelScene.uiCamera.width - 16));

		IconTitle titlebar = new IconTitle();

		Image buffIcon = new BuffIcon( buff, true );

		titlebar.icon( buffIcon );
		titlebar.label( Messages.titleCase(buff.name()), Window.TITLE_COLOR );
		titlebar.setRect( 0, 0, width, 0 );
		add( titlebar );

		ScrollingListPane list = new ScrollingListPane();
		list.setRect(0, 0, width, 1);
		for (String line : buff.desc().split("\\n", -1)) {
			list.addItem(new BuffLine(line));
		}

		float listY = titlebar.bottom() + 2*GAP;
		int maxWindowHeight = Math.max(64, PixelScene.uiCamera.height - chrome.marginVer() - 6);
		int listHeight = Math.max(24, Math.min((int)Math.ceil(list.content().height()),
				(int)Math.floor(maxWindowHeight - listY - 2)));

		list.setRect(0, listY, width, listHeight);
		add(list);
		resize(width, (int)Math.ceil(listY + listHeight + 2));
	}

	private static class BuffLine extends ScrollingListPane.ListItem {

		private final boolean spacer;

		BuffLine(String text) {
			super(null, null, text == null ? "" : text);
			spacer = text == null || text.length() == 0;
			icon.visible = false;
			iconLabel.visible = false;
			line.visible = false;
		}

		@Override
		protected void layout() {
			label.maxWidth((int)width);
			label.setPos(x, y + 1);
			height = spacer ? 6 : Math.max(8, label.height() + 3);
		}
	}
}
