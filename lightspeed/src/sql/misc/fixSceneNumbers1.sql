SET foreign_key_checks = on;

/* fixup for Jack Binder's production (P272) where scene numbers stared at "1989".
-- update scene set number = (number - 1988) where script_Id = 326 and number > 1988;
-- update strip set scene_numbers = (scene_numbers - 1988) where stripboard_id = 199 and sheet_number is not null and scene_numbers > 1988;
/**/
	