(ns export-server.browser.image-resizer
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)
           (javax.imageio ImageIO)
           (java.awt.image BufferedImage)
           (java.awt Color)))


(defn resize-image [screenshot options]
  (let [img (ImageIO/read (ByteArrayInputStream. screenshot))

        ; new-img (.getSubimage img 0 0 (:image-width options) (:image-height options))
        new-img (BufferedImage. (:image-width options) (:image-height options) (.getType img))
        g (.getGraphics new-img)
        _ (.setColor g Color/WHITE)
        _ (.fillRect g 0 0 (:image-width options) (:image-height options))
        _ (.drawImage g img 0 0
                      (min (.getWidth img) (:image-width options))
                      (min (.getHeight img) (:image-height options))
                      nil)
        _ (.dispose g)

        baos (ByteArrayOutputStream.)
        _ (ImageIO/write new-img "png" baos)
        bytes (.toByteArray baos)]
    (.close baos)
    bytes))