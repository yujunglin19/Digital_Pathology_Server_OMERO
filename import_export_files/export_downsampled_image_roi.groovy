import qupath.lib.gui.images.servers.RenderedImageServer
import qupath.lib.gui.viewer.overlays.HierarchyOverlay

// export path: export to folder "export"
String path = buildFilePath(PROJECT_BASE_DIR, 'export')
print(path)
mkdirs(path)

// create a folder for each image, containing: downsampled images (png), ROI information (geojson)
String img_folder_path = buildFilePath(path, GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName()))
print(img_folder_path)
mkdirs(img_folder_path)

// export downsampled annotated image
String img_path = buildFilePath(img_folder_path, GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName()) + '.png')
print(img_path)

// downsample factor: change to desired value
double downsample = 20

// obtain current viewer and image data
def viewer = getCurrentViewer()
print(viewer)
def imageData = getCurrentImageData()
print(imageData)

// create downsample rendered image
def server = new RenderedImageServer.Builder(imageData)
    .downsamples(downsample)
    .layers(new HierarchyOverlay(viewer.getImageRegionStore(), viewer.getOverlayOptions(), imageData))
    .build()

// save image
writeImage(server, img_path)

// export all ROIs
def annotations = getAnnotationObjects()
String anno_path = buildFilePath(img_folder_path, GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName()) + '.geojson')
print(anno_path)
// 'FEATURE_COLLECTION' is standard GeoJSON format for multiple objects
exportObjectsToGeoJson(annotations, anno_path, "FEATURE_COLLECTION")

print('Export Complete')