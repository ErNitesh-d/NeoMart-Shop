package com.ecom.util;

import java.io.UnsupportedEncodingException;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CommonUtil {

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private UserService userService;

	public Boolean sendMail(String url, String receptionEmail) throws UnsupportedEncodingException, MessagingException {
		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("nitinkhatri9898@gmail.com", "Shopping Cart");
		helper.setTo(receptionEmail);

		// Enhanced HTML email content
		String content = "<!DOCTYPE html>" + "<html lang='en'>" + "<head>" + "<meta charset='UTF-8'>"
				+ "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" + "</head>"
				+ "<body style='margin: 0; padding: 0; font-family: Arial, Helvetica, sans-serif; background-color: #f4f4f4; line-height: 1.6;'>"
				+ "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>"
				+ "<div style='background: linear-gradient(135deg, #ffffff 0%, #f9f9f9 100%); border-radius: 10px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1);'>"
				+ "<div style='background: linear-gradient(90deg, #4776E6 0%, #8E54E9 100%); padding: 20px; text-align: center; color: white;'>"
				+ "<h1 style='margin: 0; font-size: 28px; font-weight: 700;'>Password Reset Request</h1>" + "</div>"
				+ "<div style='padding: 30px; text-align: center;'>"
				+ "<h2 style='color: #333; font-size: 22px; margin: 0 0 15px;'>Hello!</h2>"
				+ "<p style='color: #666; font-size: 16px; margin: 0 0 20px;'>You’ve requested to reset your password for Shopping Cart. We’re here to help!</p>"
				+ "<p style='color: #666; font-size: 16px; margin: 0 0 30px;'>Click the button below to change your password:</p>"
				+ "<a href='http://localhost:8080" + url
				+ "' style='display: inline-block; padding: 12px 30px; background: linear-gradient(90deg, #4776E6 0%, #8E54E9 100%); color: white; text-decoration: none; border-radius: 25px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 10px rgba(71, 118, 230, 0.3); transition: transform 0.3s ease, box-shadow 0.3s ease;' onmouseover='this.style.transform=\"scale(1.05)\"; this.style.boxShadow=\"0 6px 15px rgba(71, 118, 230, 0.5)\";' onmouseout='this.style.transform=\"scale(1)\"; this.style.boxShadow=\"0 4px 10px rgba(71, 118, 230, 0.3)\";'>Change My Password</a>"
				+ "</div>"
				+ "<div style='background-color: #f1f1f1; padding: 15px; text-align: center; font-size: 12px; color: #888;'>"
				+ "<p style='margin: 0;'>If you didn’t request this, please ignore this email.</p>"
				+ "<p style='margin: 5px 0 0;'>© 2025 Shopping Cart. All rights reserved.</p>" + "</div>" + "</div>"
				+ "</div>" + "</body>" + "</html>";

		helper.setSubject("Reset Your Password - Shopping Cart");
		helper.setText(content, true); // true indicates HTML content
		mailSender.send(message);
		return true;
	}

	public static String generateUrl(HttpServletRequest request) {

		String siteUrl = request.getRequestURI().toString();
		return siteUrl.replace(request.getServletPath(), "");

	}

	public Boolean sendMailForProductOrder(ProductOrder productOrder, String status) throws Exception {

		StringBuilder msg = new StringBuilder();

		// ✅ Start HTML template with improved styling
		msg.append("<html><head><style>");
		msg.append(
				"body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; text-align: center; }");
		msg.append(
				".container { background: #fff; padding: 20px; border-radius: 10px; max-width: 600px; margin: auto; box-shadow: 0px 4px 10px rgba(0,0,0,0.1); }");
		msg.append(
				".header { background: #007bff; color: white; padding: 15px; font-size: 20px; font-weight: bold; border-radius: 10px 10px 0 0; }");
		msg.append(".content { padding: 20px; text-align: left; }");
		msg.append("table { width: 100%; border-collapse: collapse; margin-top: 10px; }");
		msg.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }");
		msg.append("th { background-color: #007bff; color: white; }");
		msg.append(
				".footer { margin-top: 20px; font-size: 14px; color: #555; text-align: center; padding: 10px; border-top: 1px solid #ddd; }");
		msg.append(".highlight { color: green; font-weight: bold; }");
		msg.append(".coupon { background: #f8f9fa; padding: 10px; border-radius: 5px; margin-top: 10px; }");
		msg.append("</style></head><body>");

		// ✅ Header Section
		msg.append("<div class='container'>");
		msg.append("<div class='header'>Order Confirmation</div>");
		msg.append("<div class='content'>");
		msg.append("<h2>Thank you for your order, <b>[[name]]</b>!</h2>");
		msg.append(
				"<p>Your order <b>#[[orderId]]</b> is currently <span class='highlight'>[[orderStatus]]</span>.</p>");

		// ✅ Product Details Table
		msg.append("<h3>Product Details:</h3>");
		msg.append("<table>");
		msg.append("<tr><th>Product Name</th><th>Category</th><th>Quantity</th><th>Price</th></tr>");

		// ✅ Product Item Details
		msg.append("<tr>");
		msg.append("<td>").append(productOrder.getProduct().getTitle()).append("</td>");
		msg.append("<td>").append(productOrder.getProduct().getCategory()).append("</td>");
		msg.append("<td>").append(productOrder.getQuantity()).append("</td>");
		msg.append("<td>₹").append(productOrder.getPrice()).append("</td>");
		msg.append("</tr>");

		msg.append("</table>"); // Close table

		// ✅ Order Summary
		msg.append("<h3>Order Summary:</h3>");
		msg.append("<p><b>Total Price:</b> ₹").append(productOrder.getPrice() * productOrder.getQuantity())
				.append("</p>");
		msg.append("<p><b>Payment Type:</b> ").append(productOrder.getPaymentType()).append("</p>");

		// ✅ Check if Coupon is Applied
		if (productOrder.getCouponsApplied() != null) {
			msg.append("<div class='coupon'>");
			msg.append("<h3>🎉 Coupon Applied!</h3>");
			msg.append("<p><b>Coupon Code:</b> ").append(productOrder.getCouponsApplied().getCode()).append("</p>");
			msg.append("<p><b>Discount:</b> ₹").append(productOrder.getPrice() - productOrder.getDiscountPrice())
					.append("</p>");
			msg.append("<p><b>Final Price:</b> ₹").append(productOrder.getTotalPrice()).append("</p>");
			msg.append("</div>");
		}

		// ✅ Footer Section
		msg.append("<div class='footer'>");
		msg.append("<p>We appreciate your business! If you have any questions, feel free to contact us.</p>");
		msg.append("<p><b>Shopping Cart</b> | support@shoppingcart.com</p>");
		msg.append("</div>");

		msg.append("</div></body></html>"); // ✅ Close HTML

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);

		helper.setFrom("nitinkhatri9898@gmail.com", "Shopping Cart");
		helper.setTo(productOrder.getOrderAddress().getEmail());

		// ✅ Replace placeholders
		String finalMessage = msg.toString();
		finalMessage = finalMessage.replace("[[name]]", productOrder.getOrderAddress().getFirstName());
		finalMessage = finalMessage.replace("[[orderId]]", productOrder.getOrderId());
		finalMessage = finalMessage.replace("[[orderStatus]]", status);

		helper.setSubject("Order Confirmation - " + productOrder.getOrderId());
		helper.setText(finalMessage, true);

		mailSender.send(message);
		return true;
	}

	public UserDtls getLoggedInUserDetails(Principal p) {
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}

}
