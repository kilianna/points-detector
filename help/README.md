# Background Remover Help

## Installation and introductory information

In order to use the microscope image processing toolkit for separating signal from noise, you will need to download the BackgroundRemover.jar file and place it in the Plugins directory, which is a subdirectory of either the ImageJ or Figi.app directory. After restarting the program, the toolkit will be readily accessible in the drop-down menu under ImageJ: **Plugins -> Background Remover**.

The file should be imported as a single image or a stack. The image (or stack) must be in a 16-bit grayscale. By default, the plugin is run for the active window.

![main_window](img/main_window.PNG)

The program has two modes: automatic and manual. In automatic mode, we can enter predetermined parameters in individual fields or load a previously saved set of parameters (Preset). After clicking OK, the program automatically searches for points and opens a new window with the resulting image. We switch to manual mode using the "**Interactive parameters tuning**" button located in the upper left part of the window.

![main_window_interactive](img/main_window_interactive.PNG)

The plugin places annotations regarding the operations performed on the file. They can be viewed by going to **Image -> Show Info...**.

![Annotations](img/annotations.PNG)

Please note that due to the specific nature of ImageJ, annotations are displayed in random order. They should be read in the order consistent with the numbering given in square brackets.

## Automatic mode

The automatic mode is easy to use and performs transformations based on user-provided parameters. These parameters can be entered manually or loaded from a file.

### Presets

The new preset can be created by selecting the **[ New ]** option from the **Presets** drop-down list. After all the fields with the set parameters have been completed, the **Save** button should be clicked. A new window will open, asking for the name of the preset being saved. Once a name is selected and confirmed, the created preset will appear in the preset drop-down list at the top of the window.

To access a previously created preset, it can be selected from the **Presets** drop-down list. The existing preset can be updated while maintaining the changes made using the **Save** button. By selecting **[Recently used]** from the drop-down list, the most recently used preset will be loaded. The preset can be deleted using the **Delete** button.

![presets](img/presets.PNG)

### Preliminary parameters

Preliminary parameters determine the size of the scanning window and the approximate size of objects in the image.

![main_window_preliminary](img/main_window_preliminary.PNG)

- **Scanning window radius** - the size of the scanning window in pixels (half the length of the side of the square);
- **Point radius** - point size in pixels (radius);
- **Background start radius** - distance from the analyzed point, above which pixels located are treated as background (this distance should be greater than **Point radius**).

### Discrimination line parameters

To separate the signal from the noise, the program uses an appropriately adjusted discrimination line with the equation:

$$
  y = a*x+b
$$

- **Slope** - slope coefficient of simple discrimination (a);
- **Y-Intercept** - the point of intersection of the discrimination line with the OY axis (b).

![main_window_line](img/main_window_line.PNG)

Jeżeli parametry te nie są znane, należy je ustalić w trybie manualnym.

### Output parameters

If these parameters are not known, they should be set in manual mode.

![main_window_output](img/main_window_output.PNG)

**Points** - point display options available in the drop-down list:
  - **White** - points displayed in white;
  - **Black** - points displayed in black;
  - **Orginal** - points displayed the same as in the original image;
  - **Degree of matching** - pixel brightness corresponds to the difference between the "point brightness" calculated by the program and the "background brightness" (the brighter the point, the more it "sticks out above the background");
  - **Net signal (average)** - the pixel value corresponds to the basic pixel value minus the background calculated as the arithmetic mean of pixels around a given point;
  - **Net signal (mode)** - the pixel value corresponds to the basic pixel value minus the background calculated as the modal value of pixels around a given point;
  - **Net signal (median)** - the pixel value corresponds to the basic pixel value minus the background calculated as the median of pixels around a given point.

![output_points](img/output_points.PNG)

If one of the **Net signal** options is selected, two additional fields will appear with parameters to set:
- **Skip pixels** - the difference between the radius of the point for which the background is calculated and the inner radius of the ring based on which the background value is calculated.
- **Take pixels** - the difference between the outer and inner radius of the ring used to calculate the background.

![net_signal_menu](img/net_signal_menu.PNG)

![BG ring](img/Background_ring.png)

When one of the three versions of "Net signal" is selected, the "Scaled" option can also be chosen, which extends the pixel values to the entire display range and increases the contrast in the image. However, it's important to note that using this option will alter both the absolute and relative relationships between individual pixel values. It is recommended to use this mainly for visual purposes.

![Net signal vs scaled](img/Net_vs_scaled.png)

- **Background** - available background display options:
  - **White** - background displayed in white;
  - **Black** - background displayed in black;
  - **Orginal** - the background displayed is the same as in the original image;
  - **Degree of matching** - pixel brightness corresponds to the difference between the "point brightness" calculated by the program and the "background brightness".

![output_background](img/output_background.PNG)

Additional options:
- **Scope**: choose whether the plugin should perform operations solely on the selected image (**Current slice**) or on all images in the stack in the active window (**All slices**).
- **Input slices**: this feature provides the option to select between a stack of processed images (**Omit**) or a stack where the resulting images are interleaved with the corresponding original images (**Include**). It is beneficial, for instance, when comparing resulting and original images or when creating masks.
- **Display range**: for the selected image (or stack), the display range can either remain unchanged (**Keep**) or be reset so that a pixel value of 0 corresponds to black (**Reset** ). This transformation will change the absolute pixel values ​​but retain their relative values. For example, if the basic display range is 1746.0 - 15951.0, where 1746.0 corresponds to the darkest pixel in the image (black), and 15951.0 corresponds to the brightest pixel (white), then after the transformation, the pixels will take values ​​in the range 0 - 14205.0. The value of each pixel in each of the images in the stack has been reduced by 1746.0, and now black pixels have a value of 0, while white pixels have a value of 14205.0.

## Manual mode
In manual mode, you can optimize input parameters and, most importantly, the discrimination line parameters. Working in manual mode is done on the currently active image (or active image in a stack). To switch to manual mode, click the "**Interactive parameters tuning**" button located in the upper right corner of the window.

![main_window_interactive](img/main_window_interactive.PNG)

After pressing this button, the program automatically goes to the manual mode menu and opens two additional windows: **Preview** and **Plot**.

![menu_manual](img/menu_manual.PNG)

In the manual mode window, you will notice that the "**Interactive parameters tuning**" button has been replaced by the "**Profile plot window**" button, which opens an additional window displaying profiles for individual points. Furthermore, in the **Discrimination line parameters** section, there are additional options related to curve fitting (**Auto fitting**).

![preview](img/preview.PNG)

The **Preview** window is a stack of two images. The first image shows a preview of the predicted result, while the second image is the original input image. This allows for a real-time comparison between the input and output images. 

The **Plot** window initially contains only a chart template used to establish the discrimination line parameters.

![plot_empty](img/plot_empty.PNG)

### Algorithm for setting line and preliminary parameters

1. Determining the initial parameters
2. Marking background areas
3. Marking of signal points
4. Establishing discrimination line parameters
5. Determining the output parameters

#### Determining the initial parameters

In the main "**Parameters**" window, input the initial conditions under "**Preliminary parameters**":

- "**Scanning window radius**" refers to the size of the scanning window in pixels (half the length of the side of the square). The window should ideally be sized so that there is at most one signal element in the scanning window each time, with a comparable amount of background surrounding it. It's important to note that the smaller the scanning window, the shorter the plugin's operating time will be.
- **Point radius** - point size in pixels (radius). For objects with symmetrical dimensions, please enter their radius in pixels. If the objects are of different sizes, enter the value for the smallest one. If the horizontal and vertical dimensions of the objects you are looking for are significantly different, please provide the value of the smaller dimension. The specified radius must be smaller than the **Scanning window radius**.
- **Background start radius** - distance from the analyzed point, above which pixels located are treated as background. This distance should be greater than **Point radius** and smaller than the **Scanning window radius**.

#### Marking background areas

Select the areas in the image that you consider to be the background. Make sure not to include the part related to the signal. It's best to choose areas with the most varied intensities and those close to the signal we want to isolate.

The default selection tool in ImageJ for choosing background areas is the oval. You have the option to switch this tool to any other selection tools in ImageJ (Rectangle, Polygon selections, Freehand selections). It is possible to use different tools on different areas of the image. When you need to select more than one area, simply use the SHIFT key. You can move the previously selected area using the mouse. To delete selected background areas, use the right ALT key. Additionally, you can access the tools from **Edit -> Selection**.

In the Preview window, you can select the background both in the original image and in the preview image.

![background_selection](img/background_selection.PNG)

Sample image with the selected background

The selected background points will appear on the plot as blue circles.

![background_selection_plot](img/background_selection_plot.PNG)

Background points plotted on the graph (blue points)

![background_plot_correlation](img/background_plot_correlation.PNG)

Background points selected in the image transferred to the chart.

#### Marking of signal points

To switch from the background selection mode to the point selection mode, click on the **Points selection** button in the **Preview** window. When you do this, the selected background areas will disappear. If you want to go back to the background selection mode, simply click on the **Background selection** button. 

Points can be selected using the ImageJ **Multi-point** tool. After marking the points, you can move them using the mouse. To delete a selected point, click on it while pressing the left or right ALT key. Just like selecting the background, points can be selected in both the original image and in the preview.

![points_selection](img/points_selection.PNG)

Sample image with the selected signal points

The graph will display yellow circles corresponding to the points selected in the image. The numbers on the image will match with the numbers on the chart, allowing you to make adjustments if needed.

![points_selection_plot](img/points_selection_graph.PNG)

Signal points plotted on the graph (yellow points)

In order to accurately define the boundary of objects, it is recommended to mark points near the edge of the object.

![points_selection_zoom](img/points_selection_zoom.PNG)

Example of marking a point on the boundary of an object.

#### Establishing discrimination line parameters

After selecting both the background and the signal in the **Preview** window, you will be able to view the corresponding points on the graph. These points can be used to draw a discrimination line, which is represented by a yellow line on the chart. You can adjust the position of this line by dragging it with the mouse or by changing its slope with the mouse. Alternatively, you can enter specific numerical values for the line's parameters in the **Parameters** window. The equation of a straight line is:

$$
  y = a*x+b
$$

- **Slope** - slope coefficient of simple discrimination (a);
- **Y-Intercept** - the point of intersection of the discrimination line with the OY axis (b).

The second method is particularly useful when we require a specific slope for the adjusted straight line.

Trzecią opcją jest użycie jednego z trzech przycisków w oknie **Preview** w sekccji **Discrimination line**:
- **Below points** - prosta dopasowywana jest 
- **Middle** - 
-**Above noise** -
  
![discrimination_line_above_noise](img/dicrimination_line_above_noise.PNG)

![discrimination_line_below_points](img/dicrimination_line_below_points.PNG)

![discrimination_line_middle](img/dicrimination_line_middle.PNG)

#### Determining the output parameters

![BW_preview](img/BW_preview.PNG)

![WB_preview](img/WB_preview.PNG)

![original_preview](img/orginal_preview.PNG)

![degree_of_matching_preview](img/degree_of_marching_preview.PNG)

![net_signal_median_preview](img/net_signal_median_preview.PNG)
