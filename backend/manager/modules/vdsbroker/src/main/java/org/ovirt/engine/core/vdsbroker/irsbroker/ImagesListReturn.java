package org.ovirt.engine.core.vdsbroker.irsbroker;

import java.util.Map;

public final class ImagesListReturn extends StatusReturn {
    private static final String IMAGES_LIST = "imageslist";
    private String[] imageList;

    public ImagesListReturn(Map<String, Object> innerMap) {
        super(innerMap);
        Object[] tempObj = (Object[]) innerMap.get(IMAGES_LIST);
        if (tempObj != null) {
            imageList = new String[tempObj.length];
            for (int i = 0; i < tempObj.length; i++) {
                imageList[i] = (String) tempObj[i];
            }
        }
    }

    public String[] getImageList() {
        return imageList;
    }
}
