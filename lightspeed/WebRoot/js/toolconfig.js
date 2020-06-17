/* ICEfaces 3.2 version: */
CKEDITOR.editorConfig = function( config )
{
   config.resize_enabled = false;	// remove resizing handle at bottom right
   config.toolbarCanCollapse = false; // disable collapsing toolbar area
   config.toolbar = 'richTxtTbar';
   config.toolbar_richTxtTbar = [['Bold', 'Italic', 'Underline']];
};

/* ICEfaces 1.8.2 version:
FCKConfig.ToolbarSets["richTxtTbar"] = [['Bold','Italic', 'Underline']];
*/
