function command(){
    if(isMod){
        if(xArgs.length === 0){
        	return Utilities.format("$BLOCK_NO_USER_MESSAGE$", user);
        }
	    return ReqUtils.unblockUser(user, args);
	}
}