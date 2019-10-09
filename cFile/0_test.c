void default_event_handler(
    GuiWidget *widget, GuiEvent *event )
{
    char *ptr;
    int y;
    widget = ptr;
    int px, py, i;
    px = 0;
    y = 4;
    switch ( event->type ) {
        case GUI_DESTROY:
            if ( widget->spec.edit.buffer )
                free( widget->spec.edit.buffer );
            if ( widget->spec.edit.display_buffer )
                free( widget->spec.edit.display_buffer );
            break;
        case GUI_DRAW:
            /* display surface */
            stk_surface_blit( 
                widget->surface, 0, 0, -1, -1, 
                stk_display, 
                widget->screen_region.x,
                widget->screen_region.y );
            /* add text */
            gui_theme->edit_font->align = STK_FONT_ALIGN_LEFT;
            ptr = widget->spec.edit.display_buffer;
            px = widget->screen_region.x + widget->border;
            py = widget->screen_region.y + widget->border + 
                 widget->spec.edit.y_offset;
            for ( i = 0; i < widget->spec.edit.height; 
                  i++, py += gui_theme->edit_font->height,
                  ptr += widget->spec.edit.width + 1 )
                stk_font_write( gui_theme->edit_font,
                    stk_display, px, py, -1, ptr );
            /* add cursor */
            if ( widget == gui_key_widget || widget->focused )
            if ( gui_edit_blink )
                stk_surface_fill( stk_display, 
                    px + widget->spec.edit.x * 
                    gui_theme->edit_font->width,
                    widget->screen_region.y + 
                    widget->border + widget->spec.edit.y_offset + 
                    widget->spec.edit.y * 
                    gui_theme->edit_font->height,
                    1, gui_theme->edit_font->height,
                    0xffffff );
            break;
        case GUI_KEY_PRESSED:
            if ( gui_edit_keysym != event->key.keysym ) {
                gui_edit_delay = 250;
                gui_edit_handle_key( widget, 
                    event->key.keysym,
                    event->key.unicode );
                //stk_sound_play( gui_theme->type_sound );
            }
            break;
        case GUI_KEY_RELEASED:
            gui_edit_keysym = -1;
            break;
        case GUI_FOCUS_OUT:
            gui_edit_blink = 0;
            gui_widget_draw( widget );
            break;
        case GUI_CLICKED:
            stk_sound_play( gui_theme->click_sound );
            px = event->button.x - widget->screen_region.x - 
                widget->border;
            py = event->button.y - widget->screen_region.y -
                widget->border - widget->spec.edit.y_offset;
            gui_edit_adjust_cursor( widget, 
                (py / gui_theme->edit_font->height) * 
                widget->spec.edit.width + 
                (px / gui_theme->edit_font->width) + 
                widget->spec.edit.start - widget->spec.edit.pos + 1 );
            gui_edit_blink = 1;
            gui_edit_blink_time = 0;
            gui_widget_draw( widget );
            break;
    }
}