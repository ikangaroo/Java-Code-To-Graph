buildToken( char *text )
#else
buildToken( text )
char *text;
#endif
{
	Junction *j1, *j2;
	Graph g;
	TokNode *t;
	require(text!=NULL, "buildToken: invalid token name");
	
	j1 = newJunction();
	j2 = newJunction();
	t = newTokNode();
	t->altstart = CurAltStart;
	if ( *text == '"' ) {t->label=FALSE; t->token = addTexpr( text );}
	else {t->label=TRUE; t->token = addTname( text );}
	j1->p1 = (Node *) t;
	t->next = (Node *) j2;
	g.left = (Node *) j1; g.right = (Node *) j2;
	return g;
}