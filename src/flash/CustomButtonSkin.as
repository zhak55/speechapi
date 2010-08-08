package
{
	import flash.display.GradientType;
	
	import mx.skins.halo.ButtonSkin;
	
	public class CustomButtonSkin extends ButtonSkin
	{
		public function CustomButtonSkin()
		{
			super();
		}
		private var disabled:Boolean=false;
		public function setAutomatic(a:Boolean):void
		{
			disabled = a;
		}
		private var green:Boolean=true;
		override protected function updateDisplayList(w:Number, h:Number):void
		{
			super.updateDisplayList(w,h);
/*			if(name=="overSkin")
				return;
			if(name=="selectedOverSkin")
				return;
			if(name=="selectedUpSkin")
				return;*/
			// User-defined styles.
			var outBorderColors:Array     = getStyle("outBorderColors");
			var inBorderColors:Array     = getStyle("inBorderColors");
			
			var cornerRadius:Number     = getStyle("cornerRadius");
			
			var upFillAlphas:Array         = getStyle("upFillAlphas");
			var upFillColors:Array         = getStyle("upFillColors");
			var upFillRatios:Array        = getStyle("upFillRatios");
			
			var overFillAlphas:Array    = getStyle("overFillAlphas");
			var overFillColors:Array    = getStyle("overFillColors");
			var overFillRatios:Array    = getStyle("overFillRatios");
			
			var downFillAlphas:Array    = getStyle("downFillAlphas");
			var downFillColors:Array    = getStyle("downFillColors");
			var downFillRatios:Array    = getStyle("downFillRatios");

			var disabledFillAlphas:Array    = getStyle("disabledFillAlphas");
			var disabledFillColors:Array    = getStyle("disabledFillColors");
			var disabledFillRatios:Array    = getStyle("disabledFillRatios");

			var cr:Number = Math.max(0, cornerRadius);
			var cr1:Number = Math.max(0, cornerRadius - 1);
			var cr2:Number = Math.max(0, cornerRadius - 2);
			
			
/*			graphics.clear();
			
			// button border/edge
			drawRoundRect(
				0, 0, w, h, cr,
				outBorderColors, [1,1],
				verticalGradientMatrix(0, 0, w, h ),
				GradientType.LINEAR, [0,255]); 
			drawRoundRect(
				1, 1, w-2, h-2, cr1,
				inBorderColors, [1,1],
				verticalGradientMatrix(0, 0, w, h ),
				GradientType.LINEAR, [0,255]); 
*/			

			if(disabled)
			{
				drawRoundRect(
					2, 2, w - 4, h - 4, cr2,
					disabledFillColors, disabledFillAlphas,
					verticalGradientMatrix(0, 0, w - 2, h - 2),
					GradientType.LINEAR, disabledFillRatios); 			
				return;
			}
			switch (name)
			{            
				case "selectedUpSkin":
				case "selectedOverSkin":
				{
					green=false;
					// button fill
					drawRoundRect(
						2, 2, w - 4, h - 4, cr2,
						downFillColors, downFillAlphas,
						verticalGradientMatrix(0, 0, w - 2, h - 2),
						GradientType.LINEAR, downFillRatios); 
					
					break;
				}

				//green
				case "upSkin":
				{
					green = true;
					// button fill
					drawRoundRect(
						2, 2, w - 4, h - 4, cr2,
						upFillColors, upFillAlphas,
						verticalGradientMatrix(0, 0, w - 2, h - 2),
						GradientType.LINEAR, upFillRatios);
					break;
				}
					
				case "overSkin":
				{
					// button fill
					if(green)
					{
						drawRoundRect(
							2, 2, w - 4, h - 4, cr2,
							upFillColors, upFillAlphas,
							verticalGradientMatrix(0, 0, w - 2, h - 2),
							GradientType.LINEAR, upFillRatios);
					}
					else
					{
						drawRoundRect(
							2, 2, w - 4, h - 4, cr2,
							downFillColors, downFillAlphas,
							verticalGradientMatrix(0, 0, w - 2, h - 2),
							GradientType.LINEAR, downFillRatios);
						green = false;
					}
					break;
				}
					
				case "downSkin":
				case "selectedDownSkin":
				{
					// button fill
					drawRoundRect(
						2, 2, w - 4, h - 4, cr2,
						downFillColors, downFillAlphas,
						verticalGradientMatrix(0, 0, w - 2, h - 2),
						GradientType.LINEAR, downFillRatios);
					green = false;
					break; 
				}                
			}
		}
		
	}
}
