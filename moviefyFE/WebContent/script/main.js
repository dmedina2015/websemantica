$(document).ready(function(){
  $('.header').height($(window).height());
})

function add(ths,sno){

	for (var i=1;i<=5;i++){
		var cur=document.getElementById("star"+i)
		cur.className="fa fa-star"
	}
		
	for (var i=1;i<=sno;i++){
		var cur=document.getElementById("star"+i)
		if(cur.className=="fa fa-star")
		{
			cur.className="fa fa-star checked"
		}
	}

}