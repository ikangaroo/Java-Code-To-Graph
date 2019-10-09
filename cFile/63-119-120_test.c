void cmd_addhist(mlist, cmd, cs)
{
#if CMD_HISTORY
	struct mlist *ml;

	/*
	 * Don't save a trivial command.
	 */
	if (strlen_cs(cmd, cs) == 0)
		return;
	/*
	 * Don't save if a duplicate of a command which is already
	 * in the history.
	 * But select the one already in the history to be current.
	 */
	for (ml = mlist->next;  ml != mlist;  ml = ml->next)
	{
		if (strcmp(ml->string, cmd) == 0)
			break;
	}
	if (ml == mlist)
	{
		/*
		 * Did not find command in history.
		 * Save the command and put it at the end of the history list.
		 */
		ml = (struct mlist *) ecalloc(1, sizeof(struct mlist));
		ml->string = strdup_cs(cmd, cs, &ml->charset);
		ml->next = mlist;
		ml->prev = mlist->prev;
		mlist->prev->next = ml;
		mlist->prev = ml;
	}
	/*
	 * Point to the cmd just after the just-accepted command.
	 * Thus, an UPARROW will always retrieve the previous command.
	 */
	mlist->curr_mp = ml->next;
#endif
}