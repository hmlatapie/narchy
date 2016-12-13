/*******************************************************************************
 * Copyright (c) 2016 comtel inc.
 *
 * Licensed under the Apache License, version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package spacegraph.net.vnc.rfb.render.rect;

import jcog.list.FasterList;
import spacegraph.net.vnc.rfb.codec.Encoding;

import java.util.List;

public class HextileImageRect extends ImageRect {

    protected final List<RawImageRect> rects;

    public HextileImageRect(int x, int y, int width, int height) {
        super(x, y, width, height);
        rects = new FasterList<>(0);
    }

    public List<RawImageRect> getRects() {
        return rects;
    }

    @Override
    public Encoding getEncoding() {
        return Encoding.HEXTILE;
    }

    @Override
    public boolean release() {
        rects.forEach(RawImageRect::release);
        return true;
    }

    @Override
    public String toString() {
        return "HextileImageRect [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ", rects.count=" + (rects != null ? rects.size() : "null")
                + ']';
    }
}